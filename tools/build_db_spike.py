#!/usr/bin/env python3
"""Build a small SQLite DB from public Danbooru tag/JP CSVs and measure size/query speed."""
import csv, os, sqlite3, time, unicodedata
from pathlib import Path
DATA = Path(__file__).resolve().parent.parent / "research" / "data"
OUT = Path(__file__).resolve().parent.parent / "research" / "promptdb-spike.sqlite"

def norm(s):
    s = unicodedata.normalize("NFKC", s).lower()
    out=[]
    for ch in s:
        o=ord(ch)
        out.append(chr(o-0x60) if 0x30A1<=o<=0x30F6 else ch)
    return "".join(out)

if OUT.exists(): OUT.unlink()
con=sqlite3.connect(OUT)
cur=con.cursor()
cur.executescript("""
PRAGMA journal_mode=OFF;
PRAGMA synchronous=OFF;
CREATE TABLE tags(id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE, category INTEGER, post_count INTEGER);
CREATE TABLE aliases(tag_id INTEGER, alias TEXT);
CREATE TABLE jp_terms(tag_id INTEGER, surface TEXT, norm TEXT, source TEXT);
CREATE INDEX idx_jp_norm ON jp_terms(norm);
CREATE INDEX idx_alias ON aliases(alias);
""")
tags={}
with open(DATA/'danbooru.csv',encoding='utf-8') as f:
    rows=list(csv.reader(f))
for row in rows:
    if len(row)<3: continue
    name,cat,count=row[0],int(row[1]),int(row[2])
    tags[name]=(cat,count,row[3].split(',') if len(row)>3 and row[3] else [])
t0=time.time()
for i,(name,(cat,count,aliases)) in enumerate(tags.items(),1):
    cur.execute("INSERT INTO tags(id,name,category,post_count) VALUES(?,?,?,?)",(i,name,cat,count))
    for a in aliases:
        if a: cur.execute("INSERT INTO aliases VALUES(?,?)",(i,a))
idmap={name:i for i,(name,(_,_,_)) in enumerate(tags.items(),1)}
for fn,src in [('danbooru-only-machine-jp.csv','machine'),('danbooru-jp.csv','manual')]:
    with open(DATA/fn,encoding='utf-8') as f:
        for row in csv.reader(f):
            if len(row)<2 or row[0] not in idmap or not row[1].strip(): continue
            # manual wins: delete existing machine row for this tag
            if src=='manual':
                cur.execute("DELETE FROM jp_terms WHERE tag_id=? AND source='machine'",(idmap[row[0]],))
            cur.execute("INSERT INTO jp_terms(tag_id,surface,norm,source) VALUES(?,?,?,?)",(idmap[row[0]],row[1].strip(),norm(row[1].strip()),src))
con.commit()
print(f"build={time.time()-t0:.2f}s tags={len(tags)} aliases={cur.execute('select count(*) from aliases').fetchone()[0]} jp={cur.execute('select count(*) from jp_terms').fetchone()[0]}")
t=time.perf_counter(); cur.execute("SELECT t.name,t.post_count FROM jp_terms j JOIN tags t ON t.id=j.tag_id WHERE j.norm='ぎんぱつ'"); print("query",cur.fetchall(),f"{(time.perf_counter()-t)*1000:.3f}ms")
con.close()
print(f"size={os.path.getsize(OUT)/1024/1024:.2f}MB")
