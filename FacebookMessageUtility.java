import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.LinkedHashMap;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FacebookMessageUtility {
	
	private File file_;
	private boolean isPerson1_;
	private boolean debug = false;
	
	private long startTime_ = 0;
	private long endTime_ = 0;

	private String person1_;
	private int p1Responded_;
	private ArrayList<String> messages1_;
	private Map<String, Integer> wordMap1_;

	private String person2_;
	private int p2Responded_;
	private ArrayList<String> messages2_;
	private Map<String, Integer> wordMap2_;
	
	public int TOP_LIMIT = 20;
	public int A4_WORD_COUNT = 400;
	
	public FacebookMessageUtility(File file, String person1, String person2) {
		this.file_ = file;
		this.person1_ = person1;
		this.person2_ = person2;
		this.messages1_ = new ArrayList<String>();
		this.messages2_ = new ArrayList<String>();
		this.p1Responded_ = 0;
		this.p2Responded_ = 0;
		
		if (debug) System.out.println("FacebookMessageUtility(" + file + ", " + person1 + ", " + person2 + ")");
	}
	
	public void readFile(File file) {
		startTime_ = System.nanoTime();
		
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			
			//Read file line by line
			String line;
			while ((line = br.readLine()) != null) {
				//Ignore blank lines
				if (line.trim().length() > 0) processLine(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		generateStats();
	}
	
	private void processLine(String line) {
		//Person 1
		if (line.equals(person1_)) {
			p1Responded_++;
			isPerson1_ = true;
		
		//Person 2
		} else if (line.equals(person2_)) {
			p2Responded_++;
			isPerson1_ = false;
		
		/* A message...
		 * Only process lines which do not match the following rules:
		 *
		 *  ^[A-Z]{3} \d\d:\d\d$			-	SAT 15:41
		 *  ^person1_$ 						-	person1_ (On new line)
		 *  ^person2_$ 						-	person2_ (On new line)
		 *  ^\d\d/\d\d/\d\d\d\d \d\d:\d\d$	-	16/11/2015 15:05
		 *  ^(\d|\d\d) [A-Z]+ \d\d:\d\d$	-	6 JANUARY 11:53
		 */
		} else if (!line.matches("^[A-Z]{3} \\d\\d:\\d\\d$|^" + person1_.split(" ")[0] + "$|^" + person2_.split(" ")[0] + "$|^\\d\\d/\\d\\d/\\d\\d\\d\\d \\d\\d:\\d\\d$|^(\\d|\\d\\d) [A-Z]+ \\d\\d:\\d\\d$")) {
			if (isPerson1_) {
				messages1_.add(line);
				if (debug) System.out.println(person1_ + ": " + line);

			} else {
				messages2_.add(line);
				if (debug) System.out.println(person2_ + ": " + line);
			}
		} else {
			if (debug) System.out.println("Rejected: " + line);
		}
	}
	
	private void generateStats() {
		//Number of messages
		int p1msgs = messages1_.size();
		int p2msgs = messages2_.size();
		
		//Number of words
		int p1words = numOfWords(messages1_);
		int p2words = numOfWords(messages2_);
		
		//Totals
		int totalMsgs = p1msgs + p2msgs;
		int totalWords = p1words + p2words;
		int a4Pages = totalWords / A4_WORD_COUNT;
		
		//Ratio of words person1:person2
		double ratioOfWords = (double)p1words / p2words;
		
		//Ratio of messages person1:person2
		double ratioOfMsgs = (double)p1msgs / p2msgs;
		
		//Percentage of messages written by each person
		int percentageOfMessagesByPerson1 = p1msgs * 100 / totalMsgs;
		int percentageOfMessagesByPerson2 = p2msgs * 100 / totalMsgs;
		
		//Percentage of words written by each person
		int percentageOfWordsByPerson1 = p1words * 100 / totalWords;
		int percentageOfWordsByPerson2 = p2words * 100 / totalWords;
		
		//Generate word histogram, ordered decending
		wordMap1_ = generateHashMap(messages1_);
		wordMap2_ = generateHashMap(messages2_);
		
		double[] p1AvgWordLengthResult = calcAverageWordLength(messages1_);
		double[] p2AvgWordLengthResult = calcAverageWordLength(messages2_);
		int TOTAL_WORDS = 0;
		int TOTAL_LETTERS = 1;
		int AVG_WORD_LENGTH = 2;
		
		//Print all relevant info out.
		System.out.println(
			person1_ + "\n"
			+ "\t" + "Responded " + p1Responded_ + " times." + "\n\n"
			+ "\t" + "Sent " + percentageOfMessagesByPerson1 + "% of the messages (" + p1msgs + ")." + "\n"
			+ "\t" + "Average message length " + p1words / p1msgs + " words." + "\n\n"
			+ "\t" + "Wrote " + percentageOfWordsByPerson1 + "% of the words (" + p1words + ")." + "\n"
			+ "\t" + "Average word length: " + String.format( "%.1f", p1AvgWordLengthResult[AVG_WORD_LENGTH]) + " letters." + "\n\n"
			+ "\t" + "Letters written: " + (int)p1AvgWordLengthResult[TOTAL_LETTERS] + "." + "\n\n"
			+ "\t" + "Unique words: " + wordMap1_.size() + "." + "\n\n"
			+ "\t" + "Top words: \n" + topWords(TOP_LIMIT, wordMap1_, p1words)
		);
		
		System.out.println("");
		
		System.out.println(
			person2_ + "\n"
			+ "\t" + "Responded " + p2Responded_ + " times." + "\n\n"
			+ "\t" + "Sent " + percentageOfMessagesByPerson2 + "% of the messages (" + p2msgs + ")." + "\n"
			+ "\t" + "Average message contains " + p2words / p2msgs + " words." + "\n\n"
			+ "\t" + "Wrote " + percentageOfWordsByPerson2 + "% of the words (" + p2words + ")." + "\n"
			+ "\t" + "Average word length: " + String.format( "%.1f", p2AvgWordLengthResult[AVG_WORD_LENGTH]) + " letters." + "\n\n"
			+ "\t" + "Letters written: " + (int)p2AvgWordLengthResult[TOTAL_LETTERS] + "." + "\n\n"
			+ "\t" + "Unique words: " + wordMap2_.size() + "." + "\n\n"
			+ "\t" + "Top words: \n" + topWords(TOP_LIMIT, wordMap2_, p2words)
		);
		
		System.out.println("");
		
		System.out.println(
			"Total words written: " + totalWords + " (~" + a4Pages + " a4 pages)." + "\n"
			+ "Total messages exhanged: " + totalMsgs + "."
		);
		
		endTime_ = System.nanoTime();
		double executionTime = (endTime_- startTime_) / 1000000000.0;
		double msgsPerSecond = totalMsgs / executionTime;
		double msgsPerMinute = msgsPerSecond * 60.0;
		System.out.println("\n" + totalMsgs + " messages processed in " + String.format("%.3f", executionTime) + "s, at a rate of " + (int)msgsPerSecond + " msgs per second.");
	}
	
	/** 
	 *  [0] totalNumberOfWords
	 *  [1] totalNumberOfLetters
	 *  [2] Average word length
	 */
	private double[] calcAverageWordLength(ArrayList<String> msgs) {
		double totalNumberOfWords = 0;
		double totalNumberOfLetters = 0;
		for (String msg : msgs) {
			String[] words = msg.split("\\s+");
			totalNumberOfWords += words.length;
			for (String word : words) {
				totalNumberOfLetters += word.length();
			}
		}
		
		double[] result = {totalNumberOfWords, totalNumberOfLetters, totalNumberOfLetters / (totalNumberOfWords * 1.0)};
		
		return result;
	}
	
	private String topWords(int limit, Map<String, Integer> map, int totalWords) {
		String topStr = "";
		
		int i = 0;
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			if (i < limit) {
				i++;
				topStr += "\t";
				topStr += i + " - " + entry.getKey() + " (" + entry.getValue() + ", " + String.format( "%.1f", entry.getValue() * 100.0 / totalWords) + "%)";
				topStr += "\n";
			
			} else {
				break;
			}
        }
		
		return topStr;
	}
	
	private Map<String, Integer> generateHashMap(ArrayList<String> msgs) {
		Map<String, Integer> hmap = new HashMap<String, Integer>();
		
		//For every message...
		for (int i = 0; i < msgs.size(); i++) {
			
			String[] words = msgs.get(i).split("\\s+");
			for (int j = 0; j < words.length; j++) {
				
				//Remove anything that isn't alpha
				String word = words[j].replaceAll("[^a-z A-Z]", "").toLowerCase();
				
				//Add or update key in hash map
				if (hmap.containsKey(word)) {
					int value = hmap.get(word);
					hmap.put(word, value + 1);
					
				} else {
					hmap.put(word, 1);
				}
			}//j
		}//i
		
		//Sort as decending
		Map<String, Integer> sortedMapAsc = sortByComparator(hmap, false);
		
		return sortedMapAsc;
	}
	
	private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order) {

        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
	
	public static void printMap(Map<String, Integer> map) {
        for (Entry<String, Integer> entry : map.entrySet()) {
            System.out.println("Key : " + entry.getKey() + " Value : "+ entry.getValue());
        }
    }
	
	public void printPerson1Messages() {
		for (int i = 0; i < messages1_.size(); i++) {
			System.out.println(person1_ + ": " + messages1_.get(i));
		}
	}

	public void printPerson2Messages() {
		for (int i = 0; i < messages2_.size(); i++) {
			System.out.println(person2_ + ": " + messages2_.get(i));
		}
	}
	
	private int numOfWords(ArrayList<String> msgs) {
		int words = 0;
		for (int i = 0; i < msgs.size(); i++) {
			String msg = msgs.get(i);
			if (!msg.isEmpty()) {
				words += msg.split("\\s+").length; // separate string around spaces
				if (debug) System.out.println(msg.split("\\s+").length + ": " + msg);
			}
		}
		
		return words;
	}
	
	public static void main(String[] args) {
		if (args.length  == 3) {
			
			File file = null;
			try {
				file = new File(args[0]);
			} catch (Exception e) {
				System.out.println("No such file.");
			}

			FacebookMessageUtility fbmu = new FacebookMessageUtility(file,args[1],args[2]);
			fbmu.readFile(file);
			
			//fbmu.printPerson1Messages();
			//fbmu.printPerson2Messages();
		} else {
			System.out.println("Incorrect use of args.");
		}
	}
}