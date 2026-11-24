const {chromium}=require('playwright');(async()=>{const b=await chromium.launch();const p=await b.newPage({viewport:{width:1024,height:500},deviceScaleFactor:1});
await p.goto('file://'+process.argv[2]);await p.waitForTimeout(400);await p.screenshot({path:process.argv[3]});await b.close()})();
