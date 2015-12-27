import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArrayMap.EntrySet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
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

import com.sun.javafx.sg.prism.NGWebView;
import com.sun.org.apache.xerces.internal.util.EntityResolver2Wrapper;
import com.sun.org.apache.xpath.internal.functions.Function;
import com.sun.xml.internal.ws.api.streaming.XMLStreamWriterFactory.Zephyr;

// this is a test version to
// index and test index, nothing relate to query topic tile!!!
// for query topic tile, you need to see TREC.java
public class TREC {
	static long time0=0;
	static long time1=0;
	static long time2;
	static long usetime;
	static Analyzer analyzer;
	static Directory index;
	static IndexReader reader;
    static IndexSearcher searcher;
	
	public static void main(String[] args) throws IOException, ParseException {
		System.out.println("start index .........");
		index();
		System.out.println("end index ..........");
		
		time1=System.currentTimeMillis();
		System.out.println("start search ..........");
//		String topicFile="WT10G/451-500.topics";
		String topicFile="WT10G/topics.txt";
		
		search(topicFile);
	    time2=System.currentTimeMillis();
	    usetime=time2-time1;
	    System.out.println("search use time "+usetime);
		System.out.println("end searching ...........");
		
		time1=System.currentTimeMillis();
		System.out.println("start evaluate ..........");
		evaluate();
	    time2=System.currentTimeMillis();
	    usetime=time2-time1;
	    System.out.println("evaluate use time "+usetime);
		System.out.println("end evaluate ...........");
	}
	
	public static void index(){
		File file=new File("output/index");
		if(!file.exists()){
			System.out.println("please use TREC_QueryTopicTitle.java to build index ...");
		}
	}
	
	public static void search(String queryfile) throws IOException, ParseException{
		File file=new File("output/results.txt");
		if(file.exists()){
			System.out.println("search okay ............");
			return;
		}
		
		
		//global static variable
//		Analyzer 
		analyzer = CustomAnalyzer.builder()
	    		   .withTokenizer("WhiteSpace")
	    		   .addTokenFilter("PorterStem")
	    		   .addTokenFilter("stop")
	    		   .build();
//		Directory 
		index = FSDirectory.open(Paths.get("output/index"));
//	    IndexReader 
	    reader = DirectoryReader.open(index);
//	    IndexSearcher 
	    searcher = new IndexSearcher(reader);
	    
//	    String pathstr=Paths.get(queryfile).toAbsolutePath().toString();
		BufferedReader bReader=new BufferedReader(new FileReader(queryfile));
		String line;
		int topicNum=0;
		String topicTitle="";
		//HashMap no order,  use TreeMap/SortedMap to sort by key.
		Map<Integer, String> topics=new TreeMap<Integer,String>();
		
		while((line=bReader.readLine())!=null){
			
			
			if(line.contains("<num>")){
				line=line.replaceAll("[^0-9]","");
				topicNum=Integer.parseInt(line);
			}
			else if(line.contains("<title>")){
				line=line.toLowerCase();
				line=line.replaceAll("<title>", "");
				topicTitle=line.replaceAll("[^a-z ]"," ").trim();
				
				if(topicTitle.isEmpty()){
					line=bReader.readLine();
					line=line.toLowerCase();
					line=line.replaceAll("<title>", "");
					topicTitle=line.replaceAll("[^a-z ]"," ").trim();
				}
				topics.put(topicNum, topicTitle);
			}
		}
		bReader.close();
		// the "title" arg specifies the default field to use
	    // when no field is explicitly specified in the query.

		BufferedWriter bWriter=new BufferedWriter(new FileWriter("output/results.txt"));
		
		for(Map.Entry<Integer,String> topic : topics.entrySet()){
			topicNum=topic.getKey();
			topicTitle=topic.getValue();
			
			System.out.println("topicNum="+topicNum);
			List<String> docs=KtrueDocs(topicTitle, 1000);
			for(String doc:docs){
				bWriter.write(topicNum+" "+doc+"\n");
			}
			
		}
		
		reader.close();
		bWriter.close();
	}
	
	public static List<String> KtrueDocs(String querystr,int K) throws IOException, ParseException {
		List<String> docs=new ArrayList<String>();
		
		Query q = new QueryParser("content", analyzer).parse(querystr);
	    
	    System.out.println("after propress, querystr="+q);
	    // 3. search
	    int hitsPerPage = K;

	    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
	    searcher.search(q, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    
	    // 4. display results
//	    System.out.println("Found " + hits.length + " hits.");
	    for(int i=0;i<hits.length;++i) {
	      int docId = hits[i].doc;
	      Document d = searcher.doc(docId);
	    
//	      System.out.println((i + 1) + ". docno=" + d.get("docno") + " title=" + d.get("title"));
//	      System.out.println("content=: "+d.get("content"));
	      docs.add(d.get("docno"));
	    }
	    
	    
		return docs;
	}
	
	
	public static void evaluate() throws IOException{
		String trueFile="WT10G/qrels.trec9_10";
		String resultFile="output/results.txt";
		
		BufferedReader trueBR=new BufferedReader(new FileReader(trueFile));
		BufferedReader resultBR=new BufferedReader(new FileReader(resultFile));
		
		BufferedWriter bWriter=new BufferedWriter(new FileWriter("output/true.txt"));
		Map<String, Integer> trueDoc=new TreeMap<String,Integer>();
		Map<String, Integer> trueScore2Doc=new TreeMap<String,Integer>();
		Map<String, Integer> trueScore1Doc=new TreeMap<String,Integer>();
//		Map<String, Integer> resultDoc=new TreeMap<String,Integer>();
//		List<String> trueDoc=new ArrayList<String>();
		List<String> resultDoc=new ArrayList<String>();
		String line,line1="",line2="";
		double MAP=0.0,MAP2=0.0;
		int i;
		for(i=451;i<500;i++){

			while((line=trueBR.readLine())!=null){
				//line format: 451 0 WTX001-B06-78 0
				String[] words=line.split(" ");
//				System.out.println(words);
				int n=Integer.parseInt(words[0]);
				int tag=Integer.parseInt(words[3]);
				if(tag!=0){
					System.out.println(line);
				}
				
				if(n==i&&tag!=0){
					trueDoc.put(words[2].trim().toLowerCase(),tag);
				}
				else if(n!=i){
					line1=line;
					break;
				}

			}
			
			
			while((line=resultBR.readLine())!=null){
				String[] words=line.split(" ");
				int n=Integer.parseInt(words[0]);
				if(n==i){
					resultDoc.add(words[1].trim());
				}
				else {
					line2=line;
					break;
				}

			}
			
			double AP=0.0;
			int pos=1;
			int count=1;
			for(Map.Entry<String, Integer> entry:trueDoc.entrySet()){
				if(entry.getValue()==2){
					trueScore2Doc.put(entry.getKey(), 2);
				}
				else{
					trueScore1Doc.put(entry.getKey(), 1);
				}
			}
			for(String docno : resultDoc){
				if(trueScore2Doc.containsKey(docno)){
					AP=AP+count/pos;
					count=count+1;
//					bWriter.write(i+" "+docno+" "+trueDoc.get(docno)+" hit! \n");
				}
				pos=pos+1;
			}
			MAP2=MAP2+AP;
			
			AP=0.0;
			pos=1;
			count=1;
			for(String docno : resultDoc){
				if(trueDoc.containsKey(docno)){
					AP=AP+count/pos;
					count=count+1;
					bWriter.write(i+" "+docno+" "+trueDoc.get(docno)+" hit at "+pos+" ! \n");
				}
				pos=pos+1;
			}
			bWriter.write(i+" AP="+AP+" count="+(count-1)+" total="+trueDoc.size()+"\n");
			MAP=MAP+AP;
			
			for (Map.Entry<String, Integer> entry : trueDoc.entrySet()) {  
//			    System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());  
				bWriter.write(i+" "+entry.getKey()+" "+entry.getValue()+"\n");
			}  
			trueDoc.clear();
//			trueDoc.add(line1);
			if(line1!=null&&!line1.isEmpty()){
				line=line1;
				String[] words=line.split(" ");
				int n=Integer.parseInt(words[0]);
				int tag=Integer.parseInt(words[3]);
				if(n==i+1&&tag!=0){
					trueDoc.put(words[2].trim(),tag);
				}
			}
			else{
				line1="";
			}
			
			resultDoc.clear();
//			resultDoc.add(line2);
			if(line2!=null&&!line2.isEmpty()){
				line=line2;
				String[] words=line.split(" ");
				int n=Integer.parseInt(words[0]);
				if(n==i){
					resultDoc.add(words[1].trim());
				}
			}
			else{
				line2="";
			}
		}
		bWriter.close();
		MAP=MAP/50.0;
		MAP2=MAP2/50.0;
		System.out.println("MAP="+MAP+" MAP2="+MAP2);
		
		
		for(i=500;i<550;i++){
			
		}
		
		trueBR.close();
		resultBR.close();
	}
	
}
