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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.LogMergePolicy;
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
import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public class TREC_QueryTopicTitle {
	public static int debugFlag=0;
	static long time0=0;
	static long time1=0;
	static long time2;
	static long usetime;
	
	public static void main(String[] args) throws IOException, ParseException {
		index();
	}
	private static void index() throws IOException, ParseException{
		// 0. Specify the analyzer for tokenizing text.
	    //    The same analyzer should be used for indexing and searching
	    System.out.println("start learn lucence");
//	    StandardAnalyzer analyzer = new StandardAnalyzer();
	    
//	    Analyzer analyzer=new WhitespaceAnalyzer();
	    
	    //stem, stop reduce index!!
	    Analyzer analyzer = CustomAnalyzer.builder()
	    		   .withTokenizer("WhiteSpace")
	    		   .addTokenFilter("PorterStem")
	    		   .addTokenFilter("stop")
	    		   .build();
	    
	    // 1. create the index 
//	    Directory index = new RAMDirectory();
	    Directory index = FSDirectory.open(Paths.get("output/index"));

	    IndexWriterConfig config = new IndexWriterConfig(analyzer);
	    
	    //1024,512 out of memory
	    config.setRAMBufferSizeMB(64);
	    config.setUseCompoundFile(false);
	    config.setMaxBufferedDocs(1000);
	    config.setMergePolicy(new LogDocMergePolicy());
//	    config.setCommitOnClose(true);
	    
	    IndexWriter w = new IndexWriter(index, config);
	    
	    
	    // 1.1 traverse file system
	    String rootPath="E:\\DataSet\\WT10G";
	    ArrayList<String> filelist=getFileList(rootPath);
	    
	    int fileNum=0;
	    time0=System.currentTimeMillis();
	    for(String filename:filelist){
	    	//remove info and other file
	    	if(filename.contains("WTX")&&filename.contains("B")){
	    		if(fileNum>-1){
	    			addDoc(w,filename);
	    		}
		    	fileNum=fileNum+1;
		    
		    	System.out.println("fileNum="+fileNum+"\t fileName="+filename);
	    	}
	    	else{
	    		System.out.println("fileNum="+fileNum+"\t fileName="+filename+"***********");
	    	}
	    	
	    	if(debugFlag==1){
	    		if(fileNum>=1) break;
	    	}
	    	
	    }
	    
	    w.close();
	    
	    System.out.println("start search");
	    search(index,analyzer);
	}
	
	private static void search(Directory index,Analyzer analyzer) throws ParseException, IOException{
		// 2. query
	    String querystr = "they choose to persecute";

	    // the "title" arg specifies the default field to use
	    // when no field is explicitly specified in the query.
	    Query q = new QueryParser("content", analyzer).parse(querystr);

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
	      System.out.println((i + 1) + ". docno=" + d.get("docno") + " title=" + d.get("title"));
//	      System.out.println("content=: "+d.get("content"));
	    }
	    
	    reader.close();
	}
	
	private static void addDoc(IndexWriter w, String fileName) throws IOException {
	    //"E:\\DataSet\\WT10G\\WTX001\\B01"
		File log=new File("log.txt");
		// public FileWriter(File file,boolean append)
		FileWriter writer = new FileWriter(log, debugFlag==0);
        
	    File in=new File(fileName);
	    BufferedReader reader=new BufferedReader(new FileReader(in));

	    String filestr=null,str=null,title="",docno="";
	    
	    int docnum=0;
	    int startToken=0;
	    while((str=reader.readLine())!=null){
	    	str=str.toLowerCase().trim();
	    	
	    	if(str.contains("<doc>")){
	    		filestr="";
	    		title="";
	    		docno="";
	    	}
	    	else if(str.contains("<docno>")||str.contains("</docno>")){
	    		if(str.contains("</docno>")&&str.contains("<docno>")){
	    			docno=str.replaceAll("<.*?>"," ").trim();
	    		}
	    		else if(str.contains("<docno>")){
	    			//0 ignore,1 docno,title,content
	    			startToken=1;
	    			filestr=str.replaceAll("<docno>", " ").trim();
	    		}
	    		else{
	    			startToken=0;
	    			filestr=filestr+str.replaceAll("</docno>", " ").trim();
	    			docno=filestr.trim();
	    			filestr="";
	    		}
	    	}
	    	else if(str.contains("<title>")||str.contains("</title>")){
	    		if(str.contains("</title>")&&str.contains("<title>")){
	    			title=str.replaceAll("<.*?>", " ");
		    		title=title.replaceAll("[^a-z ]"," ");
		    		title=title.replaceAll(" +", " ").trim();
		    		startToken=1;
	    		}
	    		else if(str.contains("<title>")){
	    			startToken=1;
	    			filestr=str;
	    		}
	    		else{
	    			filestr=filestr+str.replaceAll("<.*?>", " ");
	    			filestr=filestr.replaceAll("[^a-z ]", " ");
	    			title=filestr.replaceAll(" +"," ").trim();
	    			startToken=1;
	    			filestr="";
	    		}
	    	}
	    	else if(str.contains("</doc>")){
//	    		filestr=filestr+str.replace("<.*?>", " ");
	    		startToken=0;
	    		filestr=filestr.trim();
	    		
	    		//some doc is empty!!!
	    		//some title is broken!!!
	    		if(!filestr.isEmpty()){
	    			Document doc = new Document();
	    			filestr=filestr.replaceAll("<.*?>", " ");
		    		filestr=filestr.replaceAll("[^a-z ]", " ");
		    		filestr=filestr.replaceAll(" +", " ").trim();
		    		doc.add(new TextField("title", title, Field.Store.YES));
		    		doc.add(new StringField("docno",docno, Field.Store.YES));	
		    		doc.add(new TextField("content",filestr,Field.Store.YES));
	    	    	
	    	    	docnum=docnum+1;
	    	    	w.addDocument(doc);
	    		}
	    		
	    		if(debugFlag==1){
	    			writer.write("docnum="+docnum+"\n");
	    			writer.write("title="+title+"\n");
	    			writer.write("docno="+docno+"\n");
	    			writer.write("content="+filestr+"\n");
	    			
	    			System.out.println("docnum="+docnum);
	    			System.out.println("title="+title);
	    			System.out.println("docno="+docno);
	    			System.out.println("content="+filestr);
	    		}
	    	}
	    	else{
	    		if(startToken==1){
	    			filestr=filestr+str;
	    		}
	    	}	
	    }
	    
	    
	    System.out.println("docnum ="+docnum);
		time2=System.currentTimeMillis();
		usetime=time2-time1;
		time1=time2;
		System.out.println("current run time is "+usetime);
		usetime=time2-time0;
		System.out.println("total run time is "+usetime);
		
	    reader.close();
	    writer.close(); 
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
