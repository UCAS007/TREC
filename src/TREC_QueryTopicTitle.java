import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public class TREC_QueryTopicTitle {
	public static void main(String[] args) throws IOException, ParseException {
		index();
	}
	private static void index() throws IOException, ParseException{
		// 0. Specify the analyzer for tokenizing text.
	    //    The same analyzer should be used for indexing and searching
	    System.out.println("start learn lucence");
	    StandardAnalyzer analyzer = new StandardAnalyzer();

	    // 1. create the index 
//	    Directory index = new RAMDirectory();
	    Directory index = FSDirectory.open(Paths.get("output/index.txt"));

	    IndexWriterConfig config = new IndexWriterConfig(analyzer);

	    IndexWriter w = new IndexWriter(index, config);
	    
	    // 1.1 traverse file system
	    String rootPath="E:\\DataSet\\WT10G";
	    ArrayList<String> filelist=getFileList(rootPath);
	    
	    int fileNum=0;
	    for(String filename:filelist){
	    	//remove info and other file
	    	if(filename.contains("WTX")&&filename.contains("B")){
	    		addDoc(w,filename);
		    	fileNum=fileNum+1;
		    	if(fileNum%100==0){
		    		System.out.println("fileNum = "+fileNum);
		    	}
		    	
		    	System.out.println(filename);
	    	}
	    	else{
	    		System.out.println(filename+"********************************");
	    	}
	    	
	    	
	    }
	    
	    w.close();
	    
	    System.out.println("start search");
	    search(index,analyzer);
	}
	
	private static void search(Directory index,StandardAnalyzer analyzer) throws ParseException, IOException{
		// 2. query
	    String querystr = "What is a Bengals cat?";

	    // the "title" arg specifies the default field to use
	    // when no field is explicitly specified in the query.
	    Query q = new QueryParser("title", analyzer).parse(querystr);

	    // 3. search
	    int hitsPerPage = 10;
	    IndexReader reader = DirectoryReader.open(index);
	    IndexSearcher searcher = new IndexSearcher(reader);
	    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
	    searcher.search(q, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    
	    // 4. display results
	    System.out.println("Found " + hits.length + " hits.");
	    for(int i=0;i<hits.length;++i) {
	      int docId = hits[i].doc;
	      Document d = searcher.doc(docId);
	      System.out.println((i + 1) + ". " + d.get("docno") + "\t" + d.get("title"));
	    }
	    
	    reader.close();
	}
	
	private static void addDoc(IndexWriter w, String fileName) throws IOException {
	    //"E:\\DataSet\\WT10G\\WTX001\\B01"
	    File in=new File(fileName);
	    BufferedReader reader=new BufferedReader(new FileReader(in));

	    String filestr=null,str=null;
	    
	    int docnum=0,docnum_check=0;
	    while((str=reader.readLine())!=null){
	    	filestr=filestr+str.toLowerCase();
	    	
	    	int titleIdx=filestr.indexOf("</title>");
	    	String content;
	    	if(titleIdx==-1){
	    		content=filestr;
	    	}
	    	else{
	    		content=filestr.substring(titleIdx);
	    		content=content.replaceAll("<.*?>","");
	    	}
	    	
	    	
	    	if(str.toLowerCase().contains("</doc>")){
	    		Document doc = new Document();
	    		
//	    		System.out.println("docnum= "+docnum);
	    		
	    		org.jsoup.nodes.Document jsoupDoc=Jsoup.parse(filestr);
	    	    
	    	    Elements jsoupdocs=jsoupDoc.getElementsByTag("doc");
	    	    String title="",docno;
	    	    for(Element e : jsoupdocs){
	    	    	title=e.getElementsByTag("title").text();
	    	    	docno=e.getElementsByTag("docno").text();
//	    	    	System.out.println("title = "+title+"\n"+"DOCNO = "+docno);
	    	    	
	    	    	if(!title.isEmpty()){
	    	    		doc.add(new TextField("title", title, Field.Store.YES));
	    	    		doc.add(new StringField("docno",docno, Field.Store.YES));
	    	    		docnum_check+=1;

	    	    	}	
	    	    }
	    	    
	    	    if(!title.isEmpty()){
//	    	    	String rawContent=removeHtmlTag(filestr);
//	    	    	org.jsoup.nodes.Document rawContentDoc=Jsoup.parse(rawContent);
	    	    	
	    	    	
//	    	    	if(docnum==0) System.out.println(content);
	    	    	
	    	    	doc.add(new TextField("content",content,Field.Store.YES));
	    	    	
	    	    	docnum=docnum+1;
	    	    	w.addDocument(doc);
	    	    	
	    	    	if(titleIdx==-1){
		    	    	System.out.println("warning titleIdx **************************");
		    	    }
	    	    }
	    	    
	    	    
	    	    if(docnum!=docnum_check){
	    	    	System.out.println("warning docnum ******************************************");
	    	    }
	    	    
	    	   
	    	    filestr=null;
	    	}
	    }
	    
	    
	    System.out.println("docnum ="+docnum+"\t , docnum_check = "+docnum_check);
	    reader.close();
	  }
	
	public static String removeHtmlTag(String content) {
        Pattern p = Pattern.compile("<([a-zA-Z]+)[^<>]*>(.*?)</\\1>");
        Matcher m = p.matcher(content);
        if (m.find()) {
            content = content.replaceAll("<([a-zA-Z]+)[^<>]*>(.*?)</\\1>", "$2");
            content = removeHtmlTag(content);
        }
        return content;
    }
	
	public static ArrayList<String> getFileList(String rootPath){
		ArrayList<String> pathlist= new ArrayList<String>();
		File file = new File(rootPath);
		if(file.isDirectory()){
			File[] listing = file.listFiles();
            for(File f: listing){
            	ArrayList<String> sublist=getFileList(f.getAbsolutePath()); //recursively call for encountered sub-folders
                pathlist.addAll(sublist);
            }
		}
		else{
			pathlist.add(rootPath);
		}
		return pathlist;
	}
}
