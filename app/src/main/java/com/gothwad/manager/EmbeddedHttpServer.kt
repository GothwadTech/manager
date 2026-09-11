package com.gothwad.manager

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.IOException
import java.util.HashMap
import java.util.Locale

internal class EmbeddedHttpServer(
    context: Context,
    port: Int,
    private val uploadDirectory: File,
    private val token: String,
    private val listener: Listener
) : NanoHTTPD(port) {

    interface Listener {
        fun onFilesChanged()
        fun onOpenRequested(file: File)
    }

    companion object {
        private const val MAX_UPLOAD_BYTES = 2L * 1024L * 1024L * 1024L
    }

    private val appContext: Context = context.applicationContext

    override fun serve(session: IHTTPSession): Response {
        val params = session.parms
        if (token != params["token"]) {
            return text(Response.Status.FORBIDDEN, appContext.getString(R.string.http_invalid_link))
        }

        val uri = session.uri
        return try {
            if (Method.POST == session.method && "/upload" == uri) {
                handleUpload(session)
            } else if (Method.POST == session.method && "/action" == uri) {
                handleAction(session)
            } else if (Method.GET == session.method && "/" == uri) {
                html(renderPage())
            } else {
                text(Response.Status.NOT_FOUND, appContext.getString(R.string.http_not_found))
            }
        } catch (e: Exception) {
            text(Response.Status.INTERNAL_ERROR, appContext.getString(R.string.http_operation_failed, e.message))
        }
    }

    @Throws(IOException::class, ResponseException::class)
    private fun handleUpload(session: IHTTPSession): Response {
        val tempFiles = HashMap<String, String>()
        session.parseBody(tempFiles)
        var tempPath = tempFiles["file"]
        if (tempPath == null && tempFiles.isNotEmpty()) {
            tempPath = tempFiles.values.iterator().next()
        }
        if (tempPath == null) {
            return text(Response.Status.BAD_REQUEST, appContext.getString(R.string.http_no_file))
        }

        val source = File(tempPath)
        if (source.length() > MAX_UPLOAD_BYTES) {
            return text(Response.Status.BAD_REQUEST, appContext.getString(R.string.http_file_too_large))
        }
        if (uploadDirectory.usableSpace < source.length() + 1024L * 1024L) {
            return text(Response.Status.INTERNAL_ERROR, appContext.getString(R.string.http_insufficient_space))
        }

        var requestedName = session.parms["filename"] ?: session.parms["file"]
        val destination = FileUtils.safeDestination(
            uploadDirectory,
            requestedName,
            appContext.getString(R.string.invalid_file_name),
            appContext.getString(R.string.too_many_duplicate_names)
        )
        val partial = File(uploadDirectory, ".${destination.name}.part")
        if (partial.exists()) partial.delete()
        try {
            FileUtils.copy(source, partial)
            if (!partial.renameTo(destination)) {
                FileUtils.copy(partial, destination)
                partial.delete()
            }
        } catch (e: IOException) {
            partial.delete()
            destination.delete()
            throw e
        }
        listener.onFilesChanged()
        return text(Response.Status.OK, appContext.getString(R.string.http_upload_success, destination.name))
    }

    @Throws(IOException::class, ResponseException::class)
    private fun handleAction(session: IHTTPSession): Response {
        session.parseBody(HashMap<String, String>())
        val action = session.parms["action"]
        val requestedName = session.parms["name"]
        val file = findExisting(requestedName)
            ?: return text(Response.Status.NOT_FOUND, appContext.getString(R.string.file_not_found))

        if ("open" == action) {
            listener.onOpenRequested(file)
            return text(Response.Status.OK, appContext.getString(R.string.http_opened_on_tv))
        }
        if ("delete" == action) {
            if (!FileUtils.deleteRecursively(file)) {
                return text(Response.Status.INTERNAL_ERROR, appContext.getString(R.string.delete_failed))
            }
            listener.onFilesChanged()
            return text(Response.Status.OK, appContext.getString(R.string.http_deleted))
        }
        return text(Response.Status.BAD_REQUEST, appContext.getString(R.string.http_unknown_action))
    }

    @Throws(IOException::class)
    private fun findExisting(requestedName: String?): File? {
        if (requestedName == null) return null
        val clean = File(requestedName.replace('\\', '/')).name
        val file = File(uploadDirectory, clean)
        val parent = uploadDirectory.canonicalPath + File.separator
        if (!file.canonicalPath.startsWith(parent) || !file.exists()) return null
        return file
    }

    private fun html(body: String): Response {
        val response = newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", body)
        response.addHeader("Cache-Control", "no-store")
        response.addHeader("X-Content-Type-Options", "nosniff")
        return response
    }

    private fun text(status: Response.Status, body: String?): Response {
        return newFixedLengthResponse(status, "text/plain; charset=utf-8", body ?: "")
    }

    private fun renderPage(): String {
        val rows = StringBuilder()
        val files = FileUtils.listUploadsForDirectory(uploadDirectory)
        if (files.isEmpty()) {
            rows.append("<div class='empty'>")
                .append(htmlEscape(appContext.getString(R.string.web_no_uploads))).append("</div>")
        } else {
            for (file in files) {
                val isApk = file.name.lowercase(Locale.US).endsWith(".apk")
                rows.append("<div class='row' data-name=\"").append(attr(file.name)).append("\">")
                    .append("<div class='info'><b>").append(htmlEscape(file.name)).append("</b><small>")
                    .append(FileUtils.formatSize(file.length())).append("</small></div>")
                    .append("<div class='actions'>")
                    .append("<button onclick=\"act(this.closest('.row').dataset.name,'open')\">")
                    .append(htmlEscape(appContext.getString(if (isApk) R.string.web_install_on_tv else R.string.web_open_on_tv)))
                    .append("</button><button class='danger' onclick=\"removeFile(this.closest('.row').dataset.name)\">")
                    .append(htmlEscape(appContext.getString(R.string.permanent_delete)))
                    .append("</button></div></div>")
            }
        }

        val path = uploadDirectory.absolutePath
        return "<!doctype html><html lang='en'><head><meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<title>Gothwad Manager - " + htmlEscape(appContext.getString(R.string.remote_transfer)) + "</title><style>" +
                "*{box-sizing:border-box}body{margin:0;background:#0A0E17;color:#FFFFFF;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;-webkit-font-smoothing:antialiased}" +
                ".wrap{max-width:760px;margin:auto;padding:24px 16px}" +
                ".brand{display:flex;align-items:center;gap:12px;margin-bottom:20px}" +
                ".logo{width:40px;height:40px;border-radius:10px;background:#182338;display:flex;align-items:center;justify-content:center;font-weight:bold;color:#38BDF8;border:1px solid #2A384F}" +
                ".brand-title{font-size:20px;font-weight:bold;color:#FFFFFF}" +
                ".brand-sub{font-size:12px;color:#94A3B8;margin-left:4px}" +
                ".card{background:#121927;border:1px solid #1E293B;border-radius:16px;padding:22px;margin-bottom:22px;box-shadow:0 8px 24px rgba(0,0,0,0.35)}" +
                "h1{margin:0 0 8px;font-size:20px;font-weight:700}.path{color:#94A3B8;word-break:break-all;font-size:13px;background:#0F172A;padding:8px 12px;border-radius:8px;border:1px solid #1E293B;margin-top:10px}" +
                ".dropzone{border:2px dashed #2A384F;border-radius:12px;padding:24px 16px;text-align:center;margin-top:16px;background:#0F172A;cursor:pointer;transition:border-color .2s;display:block}" +
                ".dropzone:hover{border-color:#38BDF8}" +
                "input[type=file]{display:none}" +
                ".pick-btn{display:inline-block;padding:12px 24px;border-radius:10px;background:#0284C7;background:linear-gradient(135deg,#0284C7,#0369A1);color:#FFFFFF;font-size:15px;font-weight:bold;cursor:pointer;border:none;box-shadow:0 4px 12px rgba(2,132,199,0.3)}" +
                "button{border:0;border-radius:8px;background:#0284C7;color:white;padding:10px 14px;font-size:13px;font-weight:bold;cursor:pointer;transition:background .2s}" +
                "button:hover{background:#38BDF8}" +
                "button.danger{background:#2A1418;color:#F87171;border:1px solid #5C1D24}" +
                "button.danger:hover{background:#EF4444;color:#FFF}" +
                ".progress{height:10px;background:#1E293B;border-radius:6px;margin-top:16px;overflow:hidden}" +
                ".bar{height:100%;width:0;background:linear-gradient(90deg,#0284C7,#38BDF8);border-radius:6px;transition:width .2s}" +
                ".status{margin-top:10px;color:#94A3B8;font-size:13px;min-height:20px}" +
                "h2{margin:28px 0 14px;font-size:17px;font-weight:600;color:#94A3B8;text-transform:uppercase;letter-spacing:.5px}" +
                ".row{display:flex;align-items:center;background:#141D2E;border:1px solid #1E293B;padding:14px 16px;border-radius:12px;margin:10px 0;gap:12px}" +
                ".info{flex:1;min-width:0}.info b{display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:15px}.info small{color:#94A3B8;font-size:12px}" +
                ".actions{display:flex;gap:8px;flex-shrink:0}" +
                ".empty{text-align:center;color:#64748B;padding:36px;background:#121927;border-radius:12px;border:1px dashed #1E293B}" +
                "@media(max-width:600px){.row{flex-wrap:wrap}.info{width:100%;flex-basis:100%}.actions{width:100%;display:flex;gap:8px}.actions button{flex:1}}" +
                "</style></head><body><div class='wrap'>" +
                "<div class='brand'><div class='logo'>GM</div><div><span class='brand-title'>Gothwad Manager</span><span class='brand-sub'>by Gothwad Tech</span></div></div>" +
                "<div class='card'><h1>" + htmlEscape(appContext.getString(R.string.web_upload_heading)) + "</h1>" +
                "<div class='path'>📁 " + htmlEscape(appContext.getString(R.string.saved_path, path)) + "</div>" +
                "<label class='dropzone' for='pick'><div class='pick-btn'>＋ " + htmlEscape(appContext.getString(R.string.web_choose_files)) + "</div>" +
                "<div style='margin-top:10px;color:#64748B;font-size:12px'>Tap button to select files from your phone</div>" +
                "<input id='pick' type='file' multiple></label>" +
                "<div class='progress'><div class='bar' id='bar'></div></div>" +
                "<div class='status' id='status'></div></div>" +
                "<h2>" + htmlEscape(appContext.getString(R.string.web_uploaded_files)) + "</h2>" +
                "<div id='files'>" + rows + "</div></div><script>" +
                "const token='" + token + "',uploading='" + jsEscape(appContext.getString(R.string.web_uploading_prefix)) +
                "',networkError='" + jsEscape(appContext.getString(R.string.web_network_error)) +
                "',deletePrefix='" + jsEscape(appContext.getString(R.string.web_delete_prefix)) +
                "',deleteSuffix='" + jsEscape(appContext.getString(R.string.web_delete_suffix)) +
                "';const pick=document.getElementById('pick'),bar=document.getElementById('bar'),status=document.getElementById('status');" +
                "pick.onchange=async()=>{for(const f of pick.files){await upload(f)}setTimeout(()=>location.reload(),500)};" +
                "function upload(f){return new Promise(resolve=>{const x=new XMLHttpRequest(),d=new FormData();d.append('file',f);" +
                "x.open('POST','/upload?token='+token+'&filename='+encodeURIComponent(f.name));x.upload.onprogress=e=>{if(e.lengthComputable)bar.style.width=(e.loaded/e.total*100)+'%';status.textContent=uploading+' '+f.name+' '+Math.round(e.loaded/e.total*100)+'%'};" +
                "x.onload=()=>{status.textContent=x.responseText;resolve()};x.onerror=()=>{status.textContent=networkError;resolve()};x.send(d)})}" +
                "function act(name,action){fetch('/action?token='+token,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'action='+action+'&name='+encodeURIComponent(name)}).then(r=>r.text()).then(t=>alert(t))}" +
                "function removeFile(name){if(confirm(deletePrefix+' '+name+deleteSuffix))fetch('/action?token='+token,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'action=delete&name='+encodeURIComponent(name)}).then(r=>r.text()).then(t=>{alert(t);location.reload()})}" +
                "</script></body></html>"
    }

    private fun htmlEscape(value: String): String {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;")
    }

    private fun attr(value: String): String {
        return htmlEscape(value).replace("`", "&#96;")
    }

    private fun jsEscape(value: String): String {
        return value.replace("\\", "\\\\").replace("'", "\\'")
            .replace("\r", "\\r").replace("\n", "\\n").replace("</", "<\\/")
    }
}
