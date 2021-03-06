

import java.io.File;
import java.util.*;


public class PlagiarismDetector {

	public static class PhrasesCreator implements Runnable{
		Set<String> phrases;
		String fileName;
		int window;
		public PhrasesCreator(String fileName, int window){
			this.fileName = fileName;
			this.window = window;
		}
		@Override
		public void run() {
			this.phrases = createPhrases(this.fileName,this.window);
		}

		public Set<String> getPhrases(){return this.phrases;}
	}

	/*
	 * This method reads the given file and then converts it into a Collection of Strings.
	 * It does not include punctuation and converts all words in the file to uppercase.
	 */
	protected static List<String> readFile(String filename) {
		if (filename == null) return null;

		List<String> words = new LinkedList<String>();

		try {
			Scanner in = new Scanner(new File(filename));
			while (in.hasNext()) {
				words.add(in.next().replaceAll("[^a-zA-Z]", "").toUpperCase());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return words;
	}

	/*
	 * This method reads a file and converts it into a Set/List of distinct phrases,
	 * each of size "window". The Strings in each phrase are whitespace-separated.
	 */
	protected static Set<String> createPhrases(String filename, int window) {
		if (filename == null || window < 1) return null;

		List<String> words = readFile(filename);

		Set<String> phrases = new HashSet<String>();

		for (int i = 0; i < words.size() - window + 1; i++) {
			String phrase = "";
			for (int j = 0; j < window; j++) {
				phrase += words.get(i+j) + " ";
			}
			phrases.add(phrase);

		}
		return phrases;
	}



	public static Map<String, Integer> detectPlagiarism(String dirName, int windowSize, int threshold) {

		if(dirName == null || dirName.isEmpty()) return null;
		File dirFile = new File(dirName);
		String[] files = dirFile.list();
		if(files.length == 0) return null;

		Map<String, Integer> numberOfMatches = new HashMap<String, Integer>();
		Set<Set<String>> comparisons = new HashSet<>();
		Set<String> filePair;
		for (int i = 0; i < files.length; i++) {
			String file1 = files[i];

			for (int j = 0; j < files.length && j != i; j++) {
				String file2 = files[j];
				filePair = new HashSet<>();
				filePair.add(file1); filePair.add(file2);
				if(comparisons.contains(filePair)) continue;

				PhrasesCreator p1 = new PhrasesCreator(dirName + "/" + file1, windowSize);
				PhrasesCreator p2 = new PhrasesCreator(dirName + "/" + file2, windowSize);
				Thread t1 = new Thread(p1);
				Thread t2 = new Thread(p2);
				t1.start();
				t2.start();
				try {
					t1.join();
					t2.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Set<String> file1Phrases = p1.getPhrases();
				if(file1Phrases == null) break;
				Set<String> file2Phrases = p2.getPhrases();
				if(file2Phrases == null) continue;

				Set<String> matches = findMatches(file1Phrases, file2Phrases);
				
				if (matches == null)
					return null;
								
				if (matches.size() > threshold) {
					String key = file2 + "-" + file1;
					if (numberOfMatches.containsKey(file1 + "-" + file2) == false && file1.equals(file2) == false) {
						numberOfMatches.put(key,matches.size());
					}
				}				
			}
			
		}
		
		return sortResults(numberOfMatches);
	}
	
	/*
	 * Returns a Set of Strings that occur in both of the Set parameters.
	 * However, the comparison is case-insensitive.
	 */
	protected static Set<String> findMatches(Set<String> myPhrases, Set<String> yourPhrases) {
		if(myPhrases == null || yourPhrases == null) return null;
		myPhrases.retainAll(yourPhrases);

		return myPhrases;
	}
	
	/*
	 * Returns a LinkedHashMap in which the elements of the Map parameter
	 * are sorted according to the value of the Integer, in non-ascending order.
	 */
	protected static LinkedHashMap<String, Integer> sortResults(Map<String, Integer> possibleMatches) {
		
		LinkedHashMap<String, Integer> list = new LinkedHashMap<String, Integer>();

		for (int i = 0; i < possibleMatches.size(); i++) {
			int maxValue = 0;
			String maxKey = null;
			for (String key : possibleMatches.keySet()) {
				if (possibleMatches.get(key) > maxValue) {
					maxValue = possibleMatches.get(key);
					maxKey = key;
				}
			}
			
			list.put(maxKey, maxValue);
			
			possibleMatches.put(maxKey, -1);
		}

		return list;
	}
	
	
    public static void main(String[] args) {
    	if (args.length == 0) {
    		System.out.println("Please specify the name of the directory containing the corpus.");
    		System.exit(0);
    	}
    	String directory = args[0];
    	long start = System.currentTimeMillis();
    	Map<String, Integer> map = PlagiarismDetector.detectPlagiarism(directory, 4, 5);
    	long end = System.currentTimeMillis();
    	double timeInSeconds = (end - start) / (double)1000;
    	System.out.println("Execution time (wall clock): " + timeInSeconds + " seconds");
    	if(map == null) System.exit(-1);

    	Set<Map.Entry<String, Integer>> entries = map.entrySet();
    	for (Map.Entry<String, Integer> entry : entries) {
    		System.out.println(entry.getKey() + ": " + entry.getValue());
    	}
    }

}
