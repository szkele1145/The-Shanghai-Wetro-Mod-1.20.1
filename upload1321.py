import urllib.request
import os
import json
token = os.environ['TOKEN']
proxy = urllib.request.ProxyHandler({'http': 'http://127.0.0.1:65532', 'https': 'http://127.0.0.1:65532'})
opener = urllib.request.build_opener(proxy)
urllib.request.install_opener(opener)
base = 'https://uploads.github.com/repos/szkele1145/The-Shanghai-Wetro-Mod-1.20.1/releases/430898920/assets'
for name in ['shmetro-1.3.21-1.20.1-fabric.jar','shmetro-1.3.21-1.20.1-forge.jar']:
    path = 'fabric/build/libs/' + name if 'fabric' in name else 'forge/build/libs/' + name
    with open(path,'rb') as f:
        data = f.read()
    req = urllib.request.Request(base + '?name=' + name, data=data, method='POST')
    req.add_header('Authorization','token ' + token)
    req.add_header('User-Agent','shwetro-bot')
    req.add_header('Content-Type','application/java-archive')
    try:
        resp = urllib.request.urlopen(req)
        print(resp.read().decode())
    except Exception as e:
        print(e)
