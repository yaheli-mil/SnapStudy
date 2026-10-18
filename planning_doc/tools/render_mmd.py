# Renders every .mmd in a folder to .svg via Mermaid in headless Chrome.
import subprocess, sys, pathlib, json, html, re
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
d = pathlib.Path(sys.argv[1]).resolve()
srcs = {p.stem: p.read_text(encoding="utf-8") for p in sorted(d.glob("*.mmd"))}
page = d / "_render.html"
page.write_text("""<!doctype html><html><head><meta charset="utf-8">
<script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script></head>
<body style="margin:0;background:#fff"><div id="show"></div><script>
const SRC = %s;
mermaid.initialize({startOnLoad:false, securityLevel:'loose', theme:'base',
  fontFamily:'Arial, Helvetica, sans-serif',
  themeVariables:{fontFamily:'Arial, Helvetica, sans-serif', fontSize:'14px',
    primaryColor:'#eef2ff', primaryBorderColor:'#3b5bdb', primaryTextColor:'#111827',
    lineColor:'#475569', secondaryColor:'#f8fafc', tertiaryColor:'#ffffff'},
  class:{useMaxWidth:false}});
(async () => {
  for (const [k, code] of Object.entries(SRC)) {
    try {
      const {svg} = await mermaid.render('m_' + k, code);
      const ta = document.createElement('textarea'); ta.id = 'out_' + k; ta.textContent = svg;
      document.body.appendChild(ta);
      const div = document.createElement('div'); div.innerHTML = '<h3>'+k+'</h3>' + svg;
      document.getElementById('show').appendChild(div);
    } catch (e) {
      const ta = document.createElement('textarea'); ta.id = 'err_' + k; ta.textContent = String(e);
      document.body.appendChild(ta);
    }
  }
  document.title = 'DONE';
})();
</script></body></html>""" % json.dumps(srcs), encoding="utf-8")
r = subprocess.run([CHROME, "--headless=new", "--disable-gpu", f"--user-data-dir={d/'_prof'}",
    "--virtual-time-budget=20000", "--dump-dom", page.as_uri()], capture_output=True, timeout=180)
dom = r.stdout.decode("utf-8", "replace")
for k in srcs:
    m = re.search(r'<textarea id="out_%s">(.*?)</textarea>' % k, dom, re.S)
    if m:
        (d / f"{k}.svg").write_text(html.unescape(m.group(1)), encoding="utf-8"); print("ok", k)
    else:
        e = re.search(r'<textarea id="err_%s">(.*?)</textarea>' % k, dom, re.S)
        print("FAIL", k, html.unescape(e.group(1)) if e else dom[-500:])
