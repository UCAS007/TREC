import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gauner.jSpellCorrect.spi.ToySpellingCorrector;

/*
 * test.java
 * 
 * Copyright 2015 yzbx <yzbx@PC--20140424WEG>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 * 
 * 
 */


public class test {
	
	public static void main (String args[]) throws IOException {
		System.out.println("hello lucence");
		
		double pi=3.1415;
		System.out.println(pi);
		
		List<ResultData> list=new ArrayList<ResultData>();
		list.add(new ResultData("10#abc",0.5f));
		list.add(new ResultData("10#abc",0.6f));
		list.add(new ResultData("10#abc",0.2f));
		list.add(new ResultData(10,"abc",0.7f));
		list.add(new ResultData(10,"abc",0.1f));
		list.add(new ResultData(10,"abc",0.4f));
		
		for(ResultData rData:list){
			System.out.println(rData.score);
		}
		
//		list.sort(new FuckJava());
		list.sort(null);
		
//		Collections.sort(list);
		
		for(ResultData rData:list){
			System.out.println(rData.score);
		}
	}
	
	public static void spellTest() throws IOException{
		ToySpellingCorrector sc = new ToySpellingCorrector();
		// train some data from a text file
		sc.trainFile("WT10G/big.txt");
		// train a single word
		sc.trainSingle("some word");
		// get the best suggestion
		
//		System.out.println(sc.correct("whan did Jackie Robinson appear at his first game"));
		System.out.println(sc.correct("whan"));
		System.out.println(sc.correct("tartin"));
		System.out.println(sc.correct("bennefits"));
		System.out.println(sc.correct("compostion"));
		System.out.println(sc.correct("carribean"));
		
		String queryfile="WT10G/topics.txt";
		BufferedReader bReader=new BufferedReader(new FileReader(queryfile));
		BufferedWriter bWriter=new BufferedWriter(new FileWriter("output/title.txt"));
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
		
		for(Map.Entry<Integer, String> entry: topics.entrySet()){
			bWriter.write(entry.getValue()+"\n");
		}
		bWriter.close();
	}
}

