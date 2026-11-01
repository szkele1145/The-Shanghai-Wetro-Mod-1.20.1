import urllib.request, os, json
token=os.environ['TOKEN']
proxy=urllib.request.ProxyHandler({'http':'http://127.0.0.1:65532','https':'http://127.0.0.1:65532'})
opener=urllib.request.build_opener(proxy)
urllib.request.install_opener(opener)
base=os.environ['BASE']
for name in ['shmetro-1.3.24-1.20.1-fabric.jar','shmetro-1.3.24-1.20.1-forge.jar']:
    path='fabric/build/libs/'+name if 'fabric' in name else 'forge/build/libs/'+name
    with open(path,'rb') as f: data=f.read()
    req=urllib.request.Request(base+'?name='+name,data=data,method='POST')
    req.add_header('Authorization','token '+token)
    req.add_header('User-Agent','shwetro-bot')
    req.add_header('Content-Type','application/java-archive')
    try:
        resp=urllib.request.urlopen(req)
        print(json.loads(resp.read().decode())['browser_download_url'])
    except Exception as e:
        print(e)
