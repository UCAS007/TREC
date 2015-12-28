
public class ResultData implements Comparable<ResultData>{
	public String DOCNO;
	public float score;
	public int topicNum;
	public int docnum;
	public String key;

	public ResultData () {
		// TODO Auto-generated constructor stub
	}
	
	public ResultData(Integer topic,String docno,Float sco) {
		// TODO Auto-generated constructor stub
		topicNum=topic;
		DOCNO=docno;
		score=sco;
	}
	
	public ResultData(String tpdoc,Float sco) {
		// TODO Auto-generated constructor stub
		String[] words=tpdoc.split("#");
		
		key=tpdoc;
		topicNum=Integer.parseInt(words[0]);
		DOCNO=words[1];
		score=sco;
	}
	
//	public int compareTo(Object arg0) {
//		// TODO Auto-generated method stub
//		if(arg0 instanceof ResultData){
//			ResultData rData=(ResultData)arg0;
//			if(score>rData.score){  
//	            return 1;  
//	        }  
//	        else{  
//	            return 0;  
//	        }
//		}
//		
//		System.out.println("fuck error");
//		return -1;
//	}
	@Override
	public int compareTo(ResultData arg1) {
		// TODO Auto-generated method stub
//		ResultData rData=(ResultData)arg0;
		if(this.score < arg1.score){  
            return 1;  
        }  
        else if(this.score > arg1.score){  
            return -1;  
        }
        else{
        	return 0;
        }
//		return 0;
	}
}
