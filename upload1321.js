const https = require('https');
const fs = require('fs');
const HttpsProxyAgent = require('https-proxy-agent');
const token = process.env.TOKEN;
const agent = new HttpsProxyAgent('http://127.0.0.1:65532');
const files = [
  {name:'shmetro-1.3.21-1.20.1-fabric.jar', path:'fabric/build/libs/shmetro-1.3.21-1.20.1-fabric.jar'},
  {name:'shmetro-1.3.21-1.20.1-forge.jar', path:'forge/build/libs/shmetro-1.3.21-1.20.1-forge.jar'},
];
const base = '/repos/szkele1145/The-Shanghai-Wetro-Mod-1.20.1/releases/430898920/assets';
files.forEach(f => {
  const data = fs.readFileSync(f.path);
  const req = https.request({
    hostname: 'uploads.github.com',
    path: base + '?name=' + encodeURIComponent(f.name),
    method: 'POST',
    agent: agent,
    headers: {
      'Authorization': 'token ' + token,
      'User-Agent': 'shwetro-bot',
      'Content-Type': 'application/java-archive',
      'Content-Length': data.length
    }
  }, res => {
    let body=''; res.on('data',d=>body+=d); res.on('end',()=>{ try{ const j=JSON.parse(body); console.log(j.browser_download_url); } catch(e){ console.log(res.statusCode); } });
  });
  req.on('error',e=>console.log('ERR',e.message));
  req.write(data); req.end();
});
