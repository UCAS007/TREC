import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.gauner.jSpellCorrect.spi.ToySpellingCorrector;

// this is a test version to
// index and test index, nothing relate to query topic tile!!!
// for query topic tile, you need to see TREC.java
public class TREC {
	static long time0 = 0;
	static long time1 = 0;
	static long time2;
	static long usetime;
	static Analyzer analyzer;
	static Directory index;
	static IndexReader reader;
	static IndexSearcher searcher;
	static String systemId = "lucene5.4.0";

	public static void main(String[] args) throws IOException, ParseException {
		System.out.println("start index .........");
		index();
		System.out.println("end index ..........");

		time1 = System.currentTimeMillis();
		System.out.println("start search ..........");
		// String topicFile="WT10G/451-500.topics";
		String topicFile = "WT10G/topics.txt";

		search(topicFile);
		time2 = System.currentTimeMillis();
		usetime = time2 - time1;
		System.out.println("search use time " + usetime);
		System.out.println("end searching ...........");

		 mixQueryResults();
		
		
		 time1 = System.currentTimeMillis();
		 System.out.println("start evaluate ..........");
		 double rawMAP = evaluate("output/rawQueryResults.txt",
		 "output/result1.txt");
		 double feedBackMAP = evaluate("output/relatedQueryResults.txt",
		 "output/result2.txt");
		 double mixMAP = evaluate("output/mixQueryResults.txt",
		 "output/result3.txt");
		
		 System.out.println("rawMAP=" + rawMAP + " feedbackMAP=" +
		 feedBackMAP+" mixMAP="+mixMAP);
		 System.out.println("mixMAP="+mixMAP);
		 time2 = System.currentTimeMillis();
		 usetime = time2 - time1;
		 System.out.println("evaluate use time " + usetime);
		 System.out.println("end evaluate ...........");
		
		//output require txt
		output();
	}

	public static void index() {
		File file = new File("output/index");
		if (!file.exists()) {
			System.out.println("please use TREC_QueryTopicTitle.java to build index ...");
		}
	}

	public static void search(String queryfile) throws IOException, ParseException {
		File file = new File("output/relatedQueryResults.txt");
		if (file.exists()) {
			System.out.println("search okay ............");
			return;
		}

		// global static variable
		// Analyzer
		analyzer = CustomAnalyzer.builder().withTokenizer("WhiteSpace").addTokenFilter("PorterStem")
				.addTokenFilter("stop").build();
		// Directory
		index = FSDirectory.open(Paths.get("output/index"));
		// IndexReader
		reader = DirectoryReader.open(index);
		// IndexSearcher
		searcher = new IndexSearcher(reader);

		// String pathstr=Paths.get(queryfile).toAbsolutePath().toString();
		BufferedReader bReader = new BufferedReader(new FileReader(queryfile));
		String line;
		int topicNum = 0;
		String topicTitle = "";
		// HashMap no order, use TreeMap/SortedMap to sort by key.
		Map<Integer, String> topics = new TreeMap<Integer, String>();

		while ((line = bReader.readLine()) != null) {

			if (line.contains("<num>")) {
				line = line.replaceAll("[^0-9]", "");
				topicNum = Integer.parseInt(line);
			} else if (line.contains("<title>")) {
				line = line.toLowerCase();
				line = line.replaceAll("<title>", "");
				topicTitle = line.replaceAll("[^a-z ]", " ").trim();

				if (topicTitle.isEmpty()) {
					line = bReader.readLine();
					line = line.toLowerCase();
					line = line.replaceAll("<title>", "");
					topicTitle = line.replaceAll("[^a-z ]", " ").trim();
				}
				topics.put(topicNum, topicTitle);
			}
		}
		bReader.close();
		// the "title" arg specifies the default field to use
		// when no field is explicitly specified in the query.

		BufferedWriter bWriter = new BufferedWriter(new FileWriter("output/rawQueryResults.txt"));
		BufferedWriter bWriter2 = new BufferedWriter(new FileWriter("output/relatedQueryResults.txt"));
		// BufferedWriter bWriter3 = new BufferedWriter(new
		// FileWriter("output/result1.txt"));
		// BufferedWriter bWriter4 = new BufferedWriter(new
		// FileWriter("output/result2.txt"));

		ToySpellingCorrector sc = new ToySpellingCorrector();
		// train some data from a text file
		sc.trainFile("WT10G/big.txt");
		// train a single word
		// sc.trainSingle("some word");

		for (Map.Entry<Integer, String> topic : topics.entrySet()) {
			topicNum = topic.getKey();
			topicTitle = topic.getValue();

			String[] words = topicTitle.split(" ");
			topicTitle = "";
			for (String word : words) {
				if (!word.trim().isEmpty()) {
					String cword = sc.correct(word.trim());
					topicTitle = topicTitle + " " + cword;
				}
			}

			System.out.println("topicNum=" + topicNum + " topicTitle=" + topicTitle);
			// bWriter.write("topicNum=" + topicNum + " topicTitle=" +
			// topicTitle+"\n");

			// rank,docnum
			Map<Integer, Integer> rawQueryDocs = getRawQueryDocs(topicTitle, topicNum, bWriter);
			// Map<Integer, Integer> relatedQueryDocs =
			getRelatedQueryDocs(rawQueryDocs, topicNum, bWriter2);

			// double rawAP = evaluate(topicNum, "output/rawQueryResults.txt",
			// bWriter3);
			// double feedBackAP = evaluate(topicNum,
			// "output/relatedQueryResults.txt", bWriter4);

			// rawMAP = rawMAP + rawAP;
			// feedBackMAP = feedBackMAP + feedBackAP;
		}

		reader.close();
		bWriter.close();
		bWriter2.close();
		// bWriter3.close();
		// bWriter4.close();
	}

	public static Map<Integer, Integer> getRawQueryDocs(String querystr, int topicNum, BufferedWriter bWriter)
			throws IOException, ParseException {
		// List<String> docs = new ArrayList<String>();
		// <rank,docId>
		Map<Integer, Integer> docs = new TreeMap<Integer, Integer>();

		Query q = new QueryParser("content", analyzer).parse(querystr);

		System.out.println("after propress, querystr=" + q);
		// bWriter.write("after propress, querystr=" + q+"\n");

		// 3. search
		int hitsPerPage = 1000;

		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		System.out.println("search: " + querystr);
		System.out.println("return: " + hits.length);
		// bWriter.write("search: " + querystr+"\n");
		// bWriter.write("return: " + hits.length+"\n");

		// BufferedWriter bWriter=new BufferedWriter(new
		// FileWriter("output/formatResults.txt"));
		// 4. display results
		// System.out.println("Found " + hits.length + " hits.");
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			int rank = i + 1;
			float score = hits[i].score;

			bWriter.write(topicNum + " Q0 " + d.get("docno").toUpperCase() + " " + rank + " " + score + " " + systemId
					+ "\n");
			// System.out.println((i + 1) + ". docno=" + d.get("docno") + "
			// title=" + d.get("title"));
			// System.out.println("content=: "+d.get("content"));
			// docs.add(d.get("docno"));
			// docs.put(d.get("docno"), docId);
			docs.put(rank, docId);
		}

		return docs;
	}

	public static double evaluate(String input, String output) throws IOException {
		// String trueFile = "WT10G/qrels.trec9_10";
		String des = "output/shrinkedQueryRelatedDocs.txt";
		File file = new File(des);
		if (!file.exists()) {
			shrinkRelatedDocs();
		}

		BufferedWriter bWriter = new BufferedWriter(new FileWriter(output));
		double MAP = 0;
		BufferedReader in = new BufferedReader(new FileReader(input));
		String line1 = "";
		BufferedReader bReader = new BufferedReader(new FileReader(des));
		String line2 = "";

		for (int topicNum = 451; topicNum <= 550; topicNum++) {

			// <topicNum#DOCNO,score>
			// Map<String, Float> unsortedDocs = new TreeMap<String, Float>();
			List<ResultData> docs = new ArrayList<ResultData>();

			String line = line1;
			if (!line.isEmpty()) {
				String[] words = line.split(" ");
				int tn = Integer.parseInt(words[0]);
				if (tn == topicNum) {
					String key = words[0] + "#" + words[2];
					Float score = Float.parseFloat(words[4]);
					// unsortedDocs.put(key, score);
					ResultData rData = new ResultData(key, score);
					docs.add(rData);
				} else {
					line1 = "";
				}
			}

			while ((line = in.readLine()) != null) {
				// 452 Q0 WTX001-B04-67 540 0.25381392 lucene5.4.0
				String[] words = line.split(" ");
				int tn = Integer.parseInt(words[0]);
				if (tn == topicNum) {
					String key = words[0] + "#" + words[2];
					Float score = Float.parseFloat(words[4]);
					// unsortedDocs.put(key, score);
					ResultData rData = new ResultData(key, score);
					docs.add(rData);
				} else if (tn > topicNum) {
					line1 = line;
					break;
				}
			}

			// Map<String, Float> docs=new TreeMap<String,Float>(new
			// ValueComparator(unsortedDocs));
			// docs.putAll(unsortedDocs);
			docs.sort(null);

			// docno,level
			// upcase docno
			Map<String, Integer> maps = new TreeMap<String, Integer>();
			line = line2;
			int totalRelatedDocNum = 0;

			if (!line.isEmpty()) {
				String[] words = line.split(" ");
				int tn = Integer.parseInt(words[0]);

				if (tn == topicNum) {
					maps.put(words[2], Integer.parseInt(words[3]));
					totalRelatedDocNum = totalRelatedDocNum + 1;
				} else {
					line2 = "";
				}
			}

			while ((line = bReader.readLine()) != null) {
				String[] words = line.split(" ");
				int tn = Integer.parseInt(words[0]);

				if (tn == topicNum) {
					maps.put(words[2], Integer.parseInt(words[3]));
					totalRelatedDocNum = totalRelatedDocNum + 1;
				} else if (tn > topicNum) {
					line2 = line;
					break;
				}
			}

			double AP = 0.0;
			double count = 1.0;
			int rank = 0;
			for (int i = 0; i < docs.size(); i++) {
				// int docnum=docs.get(i);
				// Document doc=searcher.doc(docnum);
				// String docno=doc.get("docno").toUpperCase();
				String key = docs.get(i).key;
				String[] words = key.split("#");
				String docno = words[1];
				rank = rank + 1;

				if (maps.containsKey(docno)) {
					AP = AP + count / rank;
					count = count + 1;
					bWriter.write("topicNum=" + topicNum + " docno=" + docno + " rank=" + rank + " score="
							+ docs.get(i).score + "\n");
				}
			}

			AP = AP / totalRelatedDocNum;
			MAP = MAP + AP;
			bWriter.write("return " + docs.size() + " docs with " + count + " related, total related doc num is "
					+ totalRelatedDocNum + " AP=" + AP + "\n");

		}
		;

		MAP = MAP / 100;
		in.close();
		bReader.close();
		bWriter.write("MAP=" + MAP);
		bWriter.close();
		return MAP;
	}

	public static Map<Integer, Integer> getRelatedQueryDocs(Map<Integer, Integer> rawQueryDocs, int topicNum,
			BufferedWriter bWriter) throws IOException {
		// <rank,docId>
		Map<Integer, Integer> docs = new TreeMap<Integer, Integer>();

		// find high related docs in first K raw query docs;
		int K = 10;
		int docId = -1;
		int relatedLevel = 0;

		int feedBackDocId = -1;
		for (int i = 1; i <= K; i++) {
			Integer docID = rawQueryDocs.get(i);
			if (docID == null) {
				System.out.println("topicNum=" + topicNum + ": too little result in getRelatedQueryDocs");
				break;
			}
			docId = docID;

			Document document = searcher.doc(docId);
			String docno = document.get("docno");
			relatedLevel = getRelatedFeedBack(topicNum, docno);

			if (relatedLevel == 2) {
				feedBackDocId = docId;
				break;
			} else if (relatedLevel == 1) {
				if (feedBackDocId == -1) {
					feedBackDocId = docId;
				}
			}
		}

		// if not find related docs in first K raw query docs
		// just return rawQueryDocs

		if (feedBackDocId == -1) {
			return rawQueryDocs;
		}

		// do related Query
		MoreLikeThis mlt = new MoreLikeThis(reader);
		mlt.setFieldNames(new String[] { "content" });
		mlt.setAnalyzer(analyzer);

		int docnum = feedBackDocId;
		Query query = mlt.like(docnum);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1000);
		searcher.search(query, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		System.out.println("related query docnum=" + docnum + " hits.length=" + hits.length);

		int rank;
		float score;

		for (int i = 0; i < hits.length; i++) {
			docnum = hits[i].doc;
			Document document = searcher.doc(docnum);
			rank = i + 1;
			score = hits[i].score;
			bWriter.write(topicNum + " Q0 " + document.get("docno").toUpperCase() + " " + rank + " " + score + " "
					+ systemId + "\n");

			docs.put(rank, docnum);
		}

		return docs;
	}

	public static int getRelatedFeedBack(int topicNum, String docno) throws IOException {
		// TODO Auto-generated method stub
		// return related level: 0,1,2 for not related, low related, high
		// related
		String des = "output/shrinkedQueryRelatedDocs.txt";
		File file = new File(des);
		if (!file.exists()) {
			shrinkRelatedDocs();
		}
		// BufferedReader bReader=new BufferedReader(new
		// FileReader("WT10G/qrels.trec9_10"));
		BufferedReader bReader = new BufferedReader(new FileReader(des));

		int level = 0;
		String line;
		while ((line = bReader.readLine()) != null) {
			String[] words = line.split(" ");
			int topic = Integer.parseInt(words[0]);
			if (topic == topicNum) {
				if (words[2].compareToIgnoreCase(docno) == 0) {
					level = Integer.parseInt(words[3]);
					break;
				}
			} else if (topic > topicNum) {
				break;
			}
		}
		bReader.close();

		// return related level for docno on topicNum!
		return level;
	}

	public static void shrinkRelatedDocs() throws IOException {
		String src = "WT10G/qrels.trec9_10";
		String des = "output/shrinkedQueryRelatedDocs.txt";

		BufferedReader brsrc = new BufferedReader(new FileReader(src));
		BufferedWriter brdes = new BufferedWriter(new FileWriter(des));

		// Map<String, Integer> mapsrc = new TreeMap<String, Integer>();
		// Map<String, Integer> mapdes = new TreeMap<String, Integer>();

		String line;
		while ((line = brsrc.readLine()) != null) {
			String[] words = line.split(" ");
			int level = Integer.parseInt(words[3]);
			if (level != 0) {
				brdes.write(line.trim() + "\n");
			}
		}

		brsrc.close();
		brdes.close();
	}

	public static void mixQueryResults() throws IOException {
		// BufferedWriter bWriter = new BufferedWriter(new
		// FileWriter("output/rawQueryResults.txt"));
		// BufferedWriter bWriter2=new BufferedWriter(new
		// FileWriter("output/relatedQueryResults.txt"));

		BufferedWriter bWriter = new BufferedWriter(new FileWriter("output/mixQueryResults.txt"));
		BufferedReader bReader1 = new BufferedReader(new FileReader("output/rawQueryResults.txt"));
		BufferedReader bReader2 = new BufferedReader(new FileReader("output/relatedQueryResults.txt"));

		String line;

		// <topicNum#DOCNO,score>
		Map<String, Float> map1 = new TreeMap<String, Float>();
		while ((line = bReader1.readLine()) != null) {
			// 451 Q0 WTX008-B37-10 1 2.023827 lucene5.4.0
			String[] words = line.split(" ");
			// int topicNum=Integer.parseInt(words[0]);
			// String DOCNO=words[2];
			float score = Float.parseFloat(words[4]);
			String key = words[0] + "#" + words[2];

			map1.put(key, score);
		}

		// <topicNum#DOCNO,score>

		while ((line = bReader2.readLine()) != null) {
			// 451 Q0 WTX008-B37-10 1 2.023827 lucene5.4.0
			String[] words = line.split(" ");
			// int topicNum=Integer.parseInt(words[0]);
			// String DOCNO=words[2];
			float score = Float.parseFloat(words[4]);
			String key = words[0] + "#" + words[2];

			if (map1.containsKey(key)) {
				float value = score + map1.get(key);
				map1.put(key, value);
			} else {
				map1.put(key, score);
			}
		}

		for (int i = 451; i <= 550; i++) {
			// map2 is a subset for map1 in specific topicNum
			// Map<String, Float> map2 = new TreeMap<String, Float>();
			List<ResultData> sortableList = new ArrayList<ResultData>();

			for (Map.Entry<String, Float> entry : map1.entrySet()) {
				String key = entry.getKey();
				String[] words = key.split("#");
				int topicNum = Integer.parseInt(words[0]);

				if (topicNum == i) {
					// map2.put(key, entry.getValue());
					ResultData rd = new ResultData(key, entry.getValue());
					sortableList.add(rd);
				}
			}

			// Map<String, Float> sortedMap = new TreeMap<String, Float>(new
			// ValueComparator(map2));
			// sortedMap.putAll(map2);

			// this function donot sort at all!!!
			// System.out.println(sortableList.size());
			sortableList.sort(null);
			// java.util.Collections.sort(sortableList);
			// System.out.println("what's the fuck!");

			int rank = 0;
			for (int j = 0; j < sortableList.size(); j++) {
				rank = rank + 1;
				// String key = entry.getKey();
				String key = sortableList.get(j).key;
				String[] words = key.split("#");
				// int topicNum = Integer.parseInt(words[0]);

				bWriter.write(words[0] + " Q0 " + words[1] + " " + rank + " " + sortableList.get(j).score + " "
						+ systemId + "\n");

				if (rank >= 1000) {
					break;
				}
			}
		}
		bReader1.close();
		bReader2.close();
		bWriter.close();

	}
	// public static void moreLikeThis() throws IOException, ParseException {
	// analyzer =
	// CustomAnalyzer.builder().withTokenizer("WhiteSpace").addTokenFilter("PorterStem")
	// .addTokenFilter("stop").build();
	// index = FSDirectory.open(Paths.get("output/index"));
	// reader = DirectoryReader.open(index);
	// searcher = new IndexSearcher(reader);
	//
	// MoreLikeThis mlt = new MoreLikeThis(reader);
	// mlt.setFieldNames(new String[]{"content"});
	// mlt.setAnalyzer(analyzer);
	//
	//// String querystr="how e-mail bennefits businesses";
	// String querystr="nativity scenes";
	//
	// Query query=new QueryParser("content", analyzer).parse(querystr);
	//
	// // Hits hits = is.search(query);
	//
	// TopScoreDocCollector collector = TopScoreDocCollector.create(300);
	// searcher.search(query, collector);
	// ScoreDoc[] hits = collector.topDocs().scoreDocs;
	// int topicNum=464;
	// String docno="wtx089-b31-150";
	//
	// int docnum=0;
	// Document document=searcher.doc(docnum);
	// for(int i=0;i<hits.length;i++){
	// docnum=hits[i].doc;
	// document=searcher.doc(docnum);
	// if(document.get("docno").compareToIgnoreCase(docno)==0){
	// break;
	// }
	// }
	//
	//// Query like(int docNum)
	//// Return a query that will return docs like the passed lucene document
	// ID.
	//
	// System.out.println("docnum="+docnum+" docno="+document.get("docno"));
	//
	//
	// // Related Queries
	// query = mlt.like(docnum);
	// collector= TopScoreDocCollector.create(300);
	// searcher.search(query, collector);
	// hits = collector.topDocs().scoreDocs;
	// System.out.println(hits.length);
	//
	// int rank;
	// float score;
	// BufferedWriter bWriter=new BufferedWriter(new
	// FileWriter("output/related.txt"));
	// for(int i=0;i<hits.length;i++){
	// docnum=hits[i].doc;
	// document=searcher.doc(docnum);
	// rank=i+1;
	// score=hits[i].score;
	// bWriter.write(topicNum+" Q0 "+document.get("docno").toUpperCase()+
	// " "+rank+" "+score+" "+systemId+"\n");
	// }
	//
	// bWriter.close();
	// }

	public static void output() throws NumberFormatException, IOException {
		String src = "output/mixQueryResults.txt";
		String des = "output/TREC.txt";

		BufferedReader bReader = new BufferedReader(new FileReader(src));

		String line;

		BufferedWriter bWriter = new BufferedWriter(new FileWriter(des));

		while ((line = bReader.readLine()) != null) {
			String[] words = line.split(" ");
			int topicNum = Integer.parseInt(words[0]);
			if (topicNum >= 500) {
				bWriter.write(line + "\n");
			}
		}

		bReader.close();
		bWriter.close();
	}
}
