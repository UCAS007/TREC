# TREC

# file introduction
- info/docid_to_url

```
> WTX001-B01-1 http://www.ram.org:80/ramblings/movies/jimmy_hollywood.html
> WTX001-B01-10 http://sd48.mountain-inter.net:80/hss/teachers/Prothero.html
> WTX001-B01-100 http://www.ccs.org:80/hc/9607/win95.html
```

- info/homepages

```
> 007excel.com:80 WTX003-B46-130
> 128.253.7.18:80 WTX003-B15-78
> 131.100.110.12:80 WTX016-B29-136
```

- [info/README](info/README)

```
> README -	this file
> docid_to_url -	mappings: WT10g docid -> URL (\*)
> homepages -	mappings: server name -> WT10g docid
> in_links -	mappings: WT10g docid -> set of WT10g docids, whose pages 
>                          contain (incoming) links to this page (\*)
> out_links -	mappings: WT10g docid -> set of WT10g docids, whose pages 
>                          are named by (outgoing) links from this page (\*)
> servers -       server names
> url_to_docid -  mappings: URL -> WT10g docid 
> wt10g_to_vlc2 - mappings: WT10g docid -> VLC2 docid (\*)
```

- [WT10G/WTX001/B01](WT10G/WTX001/B01)

```
> <DOC>
> <DOCNO>WTX001-B01-1</DOCNO>
> <DOCOLDNO>IA001-000000-B001-3</DOCOLDNO>
```

- qrels.trec9_10

```
> 451 0 WTX001-B06-78 0
> 451 0 WTX001-B08-58 0
> 451 0 WTX001-B17-53 0
```

- 451-550.topics

```
<top>

<num> Number: 451 
<title> What is a Bengals cat? 

<desc> Description: 
Provide information on the Bengal cat breed.

<narr> Narrative: 
Item should include any information on the 
Bengal cat breed, including description, origin, 
characteristics, breeding program, names of 
breeders and catteries carrying bengals.
References which discuss bengal clubs only are
not relevant.  Discussions of bengal tigers are 
not relevant.

</top>

<top>
...
</top>

...
```