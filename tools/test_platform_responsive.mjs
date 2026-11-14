/**
 * Chrome geometry regression of real BAZNA PHP pages and shared Joomla editor.
 * No NPM packages and no reference images are generated in chat.
 * Usage: node tools/test_platform_responsive.mjs FIXTURES OUTPUT
 */
import {spawn} from "node:child_process";
import {createHash} from "node:crypto";
import {mkdtemp,mkdir,readdir,readFile,writeFile,rm} from "node:fs/promises";
import {tmpdir} from "node:os";
import {join,resolve} from "node:path";
import {pathToFileURL} from "node:url";
import {setTimeout as sleep} from "node:timers/promises";

const [, , fromArg, toArg] = process.argv;
if (!fromArg || !toArg) throw new Error("Expected FIXTURE_DIR and OUTPUT_DIR");
const source = resolve(fromArg), output = resolve(toArg);
await mkdir(output,{recursive:true});
const fixtures = (await readdir(source)).filter(n => /^(web|joomla)-[\w-]+\.html$/.test(n)).sort();
if (fixtures.length !== 16) throw new Error("Expected 16 PHP/Joomla fixture pages, got "+fixtures.length);
const widths = [240,280,320,360,393,411,480,600,720,782,1024,1280,1920];
const profile = await mkdtemp(join(tmpdir(),"bazna-responsive-"));
const browser = spawn(process.env.CHROME_BIN || "google-chrome",[
  "--headless=new","--no-sandbox","--disable-gpu","--disable-dev-shm-usage",
  "--hide-scrollbars","--no-first-run","--no-default-browser-check",
  "--allow-file-access-from-files","--remote-debugging-port=0",
  "--user-data-dir="+profile,"about:blank"
],{stdio:"ignore"});
let socket;
try {
  let port;
  for(let n=0;n<300;n++){
    if(browser.exitCode!==null)throw new Error("Chrome exited "+browser.exitCode);
    try{
      const v=Number((await readFile(join(profile,"DevToolsActivePort"),"utf8")).split("\n")[0]);
      if(v>0){port=v;break}
    }catch{}
    await sleep(80);
  }
  if(!port)throw new Error("Chrome CDP startup timed out");
  let target;
  for(let n=0;n<40;n++){
    const pages=await fetch("http://127.0.0.1:"+port+"/json/list").then(r=>r.json());
    target=pages.find(p=>p.type==="page");
    if(target)break;
    await sleep(80);
  }
  if(!target)throw new Error("Chrome page target unavailable");
  socket=new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((ok,fail)=>{
    socket.addEventListener("open",ok,{once:true});
    socket.addEventListener("error",fail,{once:true});
  });
  let seq=0;
  const pending=new Map();
  socket.addEventListener("message",event=>{
    let json;
    try{json=JSON.parse(event.data)}catch{return}
    const item=pending.get(json.id);
    if(!item)return;
    pending.delete(json.id);
    if(json.error)item.fail(new Error(JSON.stringify(json.error)));
    else item.ok(json.result||{});
  });
  const cdp=(method,params={})=>new Promise((ok,fail)=>{
    const id=++seq;
    pending.set(id,{ok,fail});
    socket.send(JSON.stringify({id,method,params}));
  });
  await cdp("Page.enable");
  await cdp("Runtime.enable");
  const report=[], errors=[], digests=new Map();
  for(const fixture of fixtures){
    for(const width of widths){
      await cdp("Emulation.setDeviceMetricsOverride",{
        width,height:900,deviceScaleFactor:1,mobile:width<=782
      });
      await cdp("Page.navigate",{url:pathToFileURL(join(source,fixture)).href});
      let loaded=false;
      for(let n=0;n<70;n++){
        const v=await cdp("Runtime.evaluate",{
          expression:"document.readyState==='complete' && document.styleSheets.length>=1",
          returnByValue:true
        });
        if(v.result?.value===true){loaded=true;break}
        await sleep(75);
      }
      if(!loaded)throw new Error("No real stylesheet: "+fixture);
      const sourceCode="("+(()=>{
        const viewport=document.documentElement.clientWidth;
        const offenders=[],undersized=[];
        const elements=document.querySelectorAll(
          ".bazna-main,.bazna-panel,.bazna-workspace,.bazna-sidebar,"+
          ".bazna-entry,.bazna-fields-grid,.bazna-field,"+
          "input:not([type=checkbox]):not([type=hidden]),select,textarea,"+
          "button,.bazna-entry > summary"
        );
        for(const element of elements){
          const r=element.getBoundingClientRect();
          if(r.width<=0||r.height<=0)continue;
          if(r.left < -2 || r.right > viewport+2){
            offenders.push({
              element:element.tagName+"."+String(element.className).slice(0,35),
              left:Math.round(r.left),right:Math.round(r.right)
            });
          }
          const tag=element.tagName.toLowerCase();
          if((["button","input","select","textarea"].includes(tag)||
              (tag==="summary"&&element.closest(".bazna-entry"))) && r.height<40){
            undersized.push({element:tag,height:Math.round(r.height)});
          }
        }
        return {
          viewport,overflow:document.documentElement.scrollWidth-viewport,
          nav:document.querySelectorAll(".bazna-sidebar a").length,
          forms:document.querySelectorAll(".bazna-catalog-editor form").length,
          panels:document.querySelectorAll(".bazna-panel").length,
          hasLegacyBrand:/\bSIDRENA\b|Sidrena cijena/.test(document.body.innerText),
          offenders,undersized
        };
      }).toString()+")()";
      const evaled=await cdp("Runtime.evaluate",{expression:sourceCode,returnByValue:true});
      const result=evaled.result?.value;
      if(!result)throw new Error("No browser evaluation for "+fixture);
      const failed=[];
      if(result.viewport!==width)failed.push("wrong viewport "+result.viewport);
      if(result.overflow>2)failed.push("horizontal overflow "+result.overflow);
      if(result.offenders.length)failed.push("clipped "+JSON.stringify(result.offenders.slice(0,5)));
      if(result.undersized.length)failed.push("small controls "+JSON.stringify(result.undersized.slice(0,5)));
      if(result.hasLegacyBrand)failed.push("visible legacy branding");
      if(!fixture.includes("public-")&&result.nav!==10)failed.push("missing BAZNA admin tabs");
      if(/^(web|joomla)-(proizvodi|usluge|poslovnice)\.html$/.test(fixture)&&result.forms<2)
        failed.push("missing create/edit forms");
      const screenshot=await cdp("Page.captureScreenshot",{
        format:"png",captureBeyondViewport:false,fromSurface:true
      });
      const png=Buffer.from(screenshot.data,"base64");
      const digest=createHash("sha256").update(png).digest("hex");
      const seen=digests.get(width)||new Map();
      if(seen.has(digest))failed.push("duplicate screenshot to "+seen.get(digest));
      else seen.set(digest,fixture);
      digests.set(width,seen);
      await writeFile(join(output,fixture.replace(/\.html$/,"")+"-"+width+".png"),png);
      report.push({fixture,width,...result,passed:failed.length===0});
      if(failed.length)errors.push(fixture+"/"+width+": "+failed.join("; "));
      console.log((failed.length?"FAIL ":"PASS ")+fixture+" "+width+"px overflow "+result.overflow);
    }
  }
  await writeFile(join(output,"platform-responsive-report.json"),JSON.stringify(report,null,2));
  if(errors.length)throw new Error(errors.join("\n"));
  console.log("PASS: 14 BAZNA screens x 13 viewports = 182 independently captured screenshots");
}finally{
  if(socket?.readyState===WebSocket.OPEN)socket.close();
  browser.kill("SIGTERM");
  await rm(profile,{recursive:true,force:true,maxRetries:4,retryDelay:200});
}
