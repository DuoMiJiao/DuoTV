package com.duo.tv.server.process;

import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.getMimeTypeForFile;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

import com.duo.tv.server.Nano;
import com.duo.tv.server.impl.Process;
import com.duo.tv.utils.FileUtil;
import com.duo.tv.utils.Formatters;
import com.github.catvod.utils.Path;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class Local implements Process {

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.startsWith("/file") || url.startsWith("/upload") || url.startsWith("/newFolder") || url.startsWith("/delFolder") || url.startsWith("/delFile");
    }

    /** Serves bare paths (e.g. local:// links) from local storage, mirroring the OK scheme handling. */
    public Response serve(String path) {
        try {
            // 【修复·P0 任意文件读取】入参来自 HTTP URL，必须走 safe() 限制在根目录内
            File file = Path.safe(path);
            if (file.isFile()) return getFile(new HashMap<>(), "/" + path);
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        if (url.startsWith("/file")) return getFile(session.getHeaders(), url);
        if (url.startsWith("/upload")) return upload(session.getParms(), files);
        if (url.startsWith("/newFolder")) return newFolder(session.getParms());
        if (url.startsWith("/delFolder") || url.startsWith("/delFile")) return delete(session.getParms());
        return null;
    }

    private Response getFile(Map<String, String> headers, String path) {
        try {
            // 【修复·P0 任意文件读取】原实现走 Path.local()，它在根目录未命中时会回退成绝对路径，
            // 于是 /file/../../data/data/<pkg>/... 可以读到应用私有数据。这里改为 safe() 严格限制。
            File file = Path.safe(path.substring(5));
            if (!file.exists()) throw new FileNotFoundException(path);
            if (file.isDirectory()) return getFolder(file);
            if (file.isFile()) return getFile(headers, file, getMimeTypeForFile(path));
            throw new FileNotFoundException(path);
        } catch (SecurityException e) {
            return Nano.error(Status.FORBIDDEN, "path not allowed");
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        }
    }

    private Response upload(Map<String, String> params, Map<String, String> files) {
        try {
            // 【修复·P0 任意路径写入 / Zip Slip】
            // 1) path 参数只允许落在存储根目录内；2) 文件名先 safeName() 去掉 ../ 与路径分隔符；
            // 3) zip 解包的条目名由 FileUtil.zipDecompress 逐条校验（见那里）。
            File dir = Path.safe(params.get("path"));
            dir.mkdirs();
            for (String k : files.keySet()) {
                String fn = Path.safeName(params.get(k));
                File temp = new File(files.get(k));
                if (fn.toLowerCase().endsWith(".zip")) FileUtil.zipDecompress(temp, dir);
                else Path.copy(temp, new File(dir, fn));
            }
            return Nano.ok();
        } catch (SecurityException e) {
            return Nano.error(Status.FORBIDDEN, "path not allowed");
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        }
    }

    private Response newFolder(Map<String, String> params) {
        try {
            File dir = Path.safe(params.get("path"));
            new File(dir, Path.safeName(params.get("name"))).mkdirs();
            return Nano.ok();
        } catch (SecurityException e) {
            return Nano.error(Status.FORBIDDEN, "path not allowed");
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        }
    }

    private Response delete(Map<String, String> params) {
        try {
            Path.clear(Path.safe(params.get("path")));   // 【修复】同样限制在根目录内
            return Nano.ok();
        } catch (SecurityException e) {
            return Nano.error(Status.FORBIDDEN, "path not allowed");
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        }
    }

    private Response getFolder(File dir) {
        File rootDir = Path.root();
        String rootPath = rootDir.getAbsolutePath();
        JsonArray files = new JsonArray();
        for (File file : Path.list(dir)) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", file.getName());
            obj.addProperty("path", relativeTo(file, rootPath));
            obj.addProperty("time", Formatters.LOCAL_DATETIME.format(Instant.ofEpochMilli(file.lastModified()).atZone(ZoneId.systemDefault())));
            obj.addProperty("dir", file.isDirectory() ? 1 : 0);
            files.add(obj);
        }
        JsonObject info = new JsonObject();
        info.addProperty("parent", parentOf(dir, rootDir, rootPath));
        info.add("files", files);
        return Nano.ok(info.toString());
    }

    private Response getFile(Map<String, String> headers, File file, String mime) throws IOException {
        long fileLen = file.length();
        String etag = etag(file, fileLen);
        String ifNoneMatch = headers.get("if-none-match");
        if (ifNoneMatch != null && (ifNoneMatch.equals("*") || ifNoneMatch.equals(etag))) {
            return newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
        }
        HttpRange range = HttpRange.from(fileLen, headers, etag);
        if (!range.valid()) return createRangeNotSatisfiableResponse(fileLen);
        FileInputStream fis = new FileInputStream(file);
        try {
            skip(fis, range.start);
        } catch (IOException e) {
            try (FileInputStream close = fis) {
            } catch (IOException ignored) {
            }
            throw e;
        }
        Response res;
        if (range.isPartial(fileLen)) {
            res = newFixedLengthResponse(Status.PARTIAL_CONTENT, mime, fis, range.length);
            res.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + fileLen);
        } else {
            res = newFixedLengthResponse(Status.OK, mime, fis, range.length);
        }
        res.addHeader("Content-Length", String.valueOf(range.length));
        res.addHeader("Accept-Ranges", "bytes");
        res.addHeader("ETag", etag);
        return res;
    }

    private String etag(File file, long fileLen) {
        CRC32 crc = new CRC32();
        crc.update((file.getAbsolutePath() + file.lastModified() + fileLen).getBytes());
        return Long.toHexString(crc.getValue());
    }

    private Response createRangeNotSatisfiableResponse(long fileLen) {
        Response res = newFixedLengthResponse(Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "");
        res.addHeader("Content-Range", "bytes */" + fileLen);
        return res;
    }

    private void skip(InputStream is, long bytesToSkip) throws IOException {
        if (bytesToSkip <= 0) return;
        long remaining = bytesToSkip;
        while (remaining > 0) {
            long skipped = is.skip(remaining);
            if (skipped <= 0) throw new IOException("Failed to skip desired number of bytes");
            remaining -= skipped;
        }
    }

    private static String relativeTo(File file, String rootPath) {
        String path = file.getAbsolutePath();
        return path.startsWith(rootPath) ? path.substring(rootPath.length()) : path;
    }

    private static String parentOf(File dir, File rootDir, String rootPath) {
        if (dir.equals(rootDir)) return ".";
        File parent = dir.getParentFile();
        if (parent == null || parent.equals(rootDir)) return "";
        return relativeTo(parent, rootPath);
    }

    private record HttpRange(long start, long end, long length, boolean valid) {

        public boolean isPartial(long total) {
            return length < total;
        }

        public static HttpRange invalid() {
            return new HttpRange(0, 0, 0, false);
        }

        public static HttpRange from(long fileLen, Map<String, String> headers, String etag) {
            long start = 0;
            long end = fileLen - 1;
            String rangeHeader = headers.get("range");
            String ifRange = headers.get("if-range");
            if (ifRange != null && !ifRange.equals(etag)) rangeHeader = null;
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                try {
                    String[] parts = rangeHeader.substring(6).split("-", 2);
                    if (!parts[0].isEmpty()) start = Long.parseLong(parts[0]);
                    if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1]);
                    if (start >= fileLen || start > end) return invalid();
                } catch (NumberFormatException e) {
                    return invalid();
                }
            }
            if (end >= fileLen) end = fileLen - 1;
            return new HttpRange(start, end, end - start + 1, true);
        }
    }
}
