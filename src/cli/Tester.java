package cli;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;

import data.RecordManager;

import objects.BPlusTree;
/**
 * Class for testing the performance of this B+-tree implementation.
 * Currently contains three test sequences with different ways of inserting index records into the
 * file.
 * This program tests the BPlusTree structure performance when subjected to different block sizes and 
 * amount of <key, value> pairs inserted and queried to/from the tree. It also validates the tree fill rate
 * according to the B+ tree definition.
 * @author Emil Bergwik
 *
 */
public class Tester {
	private static FileWriter fw;
	private static PrintWriter pw;
	private static Random rnd;
	private static HashSet<Integer> keys;

	private static BPlusTree tree;
	private static int BLOCK_SIZE;
	private static int MIN_BLOCK_SIZE = 38; 
	private static int MAX_BLOCK_SIZE = 1024;
	private static int BLOCK_INCREMENT = 32;
	private static int KEY_AMOUNT = 50000;
	private static int TREE_LEVEL;
	private static int AMOUNT_OF_RUNS = 5;
	private static int[] BLOCK_SIZES_ARRAY, KEY_AMOUNT_ARRAY;
	private static long startInsert, stopInsert, executionInsert, startQuery, stopQuery, executionQuery;
	private static String[][] RESULT_ARRAY_ONE, RESULT_ARRAY_TWO;
	private static long[][] RESULT_ARRAY_THREE;
	private static long[][] TREE_LEVELS, NUM_READ_WRITES;
	private static boolean TEST_ONE_RUN = false, TEST_TWO_RUN = false, TEST_THREE_RUN = false;
	private static String TEST_TYPE;
	private static String TEST_OUTPUT_FILE = "test_";
	private static String TEST_TYPE_ONE = "Randomized data";
	private static String TEST_TYPE_TWO = "Sequential data";
	private static String TEST_TYPE_THREE = "Read data from file";
	private static String DATABASE_FILE_NAME = "database.dat";
	private static String INDEX_FILE_NAME = "index.dat";

	public static void main(String[] args) {
		TEST_OUTPUT_FILE +=	new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) + ".txt";
		try {
			fw = new FileWriter(TEST_OUTPUT_FILE);
			pw = new PrintWriter(fw);
		} catch (IOException e) {
			e.printStackTrace();
		}
		log("---START OF TEST PROGRAM---");
		log("Generating " + ((MAX_BLOCK_SIZE-MIN_BLOCK_SIZE)/BLOCK_INCREMENT+1)*KEY_AMOUNT + " unique, random <key, value> pairs to use for testing.");
		generateData();
		RESULT_ARRAY_ONE = new String[KEY_AMOUNT_ARRAY.length][BLOCK_SIZES_ARRAY.length];
		RESULT_ARRAY_TWO = new String[KEY_AMOUNT_ARRAY.length][BLOCK_SIZES_ARRAY.length];
		RESULT_ARRAY_THREE = new long[BLOCK_SIZES_ARRAY.length][AMOUNT_OF_RUNS];
		TREE_LEVELS = new long[BLOCK_SIZES_ARRAY.length][AMOUNT_OF_RUNS];
		NUM_READ_WRITES = new long[BLOCK_SIZES_ARRAY.length][AMOUNT_OF_RUNS];
		for(int j=0;j<BLOCK_SIZES_ARRAY.length;j++){
			for(int i=0;i<AMOUNT_OF_RUNS;i++){
//				log("Running test sequence 1...");
//				RESULT_ARRAY_ONE[i][j] = testSequenceOne(KEY_AMOUNT, BLOCK_SIZES_ARRAY[j]);
				//log("Running test sequence 2...");
				//RESULT_ARRAY_TWO[i][j] = testSequenceTwo(KEY_AMOUNT_ARRAY[i], BLOCK_SIZES_ARRAY[j]);
				log("Running test sequence 3 no " + i + " ...");
				long[] runResults = testSequenceThree(KEY_AMOUNT, BLOCK_SIZES_ARRAY[j]);
				RESULT_ARRAY_THREE[j][i] = runResults[0];
				TREE_LEVELS[j][i] = runResults[1];
				NUM_READ_WRITES[j][i] = runResults[2];
			}
		}
		printResultsFromTesting();
		log("---END OF TEST PROGRAM---");
		pw.close();
	}
	/**
	 * Initializes underlying data structures and generates data used for testing.
	 */
	private static void generateData(){
		rnd = new Random();
		BLOCK_SIZES_ARRAY = new int[(MAX_BLOCK_SIZE-MIN_BLOCK_SIZE)/BLOCK_INCREMENT+1];
		BLOCK_SIZES_ARRAY[0] = MIN_BLOCK_SIZE;
		for(int i=1;i<BLOCK_SIZES_ARRAY.length;i++){
			BLOCK_SIZES_ARRAY[i] = MIN_BLOCK_SIZE + i*BLOCK_INCREMENT;
		}
		KEY_AMOUNT_ARRAY = new int[BLOCK_SIZES_ARRAY.length];
		KEY_AMOUNT_ARRAY[0] = KEY_AMOUNT;
		for(int i=1;i<KEY_AMOUNT_ARRAY.length;i++){
			KEY_AMOUNT_ARRAY[i] = (i+1)*KEY_AMOUNT;
		}
		keys = new HashSet<Integer>();
		while(keys.size() < KEY_AMOUNT_ARRAY.length){
			keys.add(rnd.nextInt());
		}
	}
	/**
	 * Generate index from randomized key insertion
	 */
	private static String testSequenceOne(int keyAmount, int blockSize){
		TEST_TYPE = TEST_TYPE_ONE;
		BLOCK_SIZE = blockSize;
		KEY_AMOUNT = keyAmount;
		tree = new BPlusTree(INDEX_FILE_NAME, BLOCK_SIZE);
		log("Test sequence information\n" +
				"Block size: " + BLOCK_SIZE + " bytes\n" +
				"Node size: " + tree.getOrder() + "\n" +
				"Amount of insertions: " + KEY_AMOUNT + "\n" +
				"Data source: " + TEST_TYPE + "\n" +
				"Description: Random key insertions with no duplicates and querying of every key."
		);
		startInsert = System.currentTimeMillis();
		int insertionCounter=0;		
		while(insertionCounter <= KEY_AMOUNT){
			for(int e: keys){
				tree.insert(e, e);
				insertionCounter++;
			}
		}
		stopInsert = System.currentTimeMillis();
		executionInsert = stopInsert - startInsert;
		log("Test sequence 1 insertion phase completed in " + executionInsert + " milliseconds.");
		startQuery = System.currentTimeMillis();
		int queryCounter = 0;
		int rootID = tree.getRoot();
		while(queryCounter <= insertionCounter){
			for(int e: keys){
				tree.get(e, rootID);
				queryCounter++;
			}
		}
		stopQuery = System.currentTimeMillis();
		executionQuery = stopQuery - startQuery;
		log("Test sequence 1 query phase completed in " + executionQuery + " milliseconds.");
		log("Inserted and queried " + KEY_AMOUNT + " keys with " + 
				tree.getFileManager().getNumberOfReadWrites() + " number of disk accesses.");
		log("Tree level: " + tree.getTreeLevel());
		log("Deleting index file for next run: "+ tree.getFileManager().deleteFile());
		log("--------------");
		TEST_ONE_RUN = true;
		return ""+executionQuery;
	}
	/**
	 * Generate index from sequential key insertion
	 */
	private static String testSequenceTwo(int keyAmount, int blockSize){
		TEST_TYPE = TEST_TYPE_TWO;
		BLOCK_SIZE = blockSize;
		KEY_AMOUNT = keyAmount;
		tree = new BPlusTree(INDEX_FILE_NAME, BLOCK_SIZE);
		log("Test sequence information\n" +
				"Block size: " + BLOCK_SIZE + " bytes\n" +
				"Node size: " + tree.getOrder() + "\n" +
				"Amount of insertions: " + KEY_AMOUNT + "\n" +
				"Data source: " + TEST_TYPE + "\n" +
				"Description: Sequential key insertions with no duplicates and querying of every key."
		);
		startInsert = System.currentTimeMillis();
		for(int i=1;i<=KEY_AMOUNT;i++){
			tree.insert(i, i);
		}
		stopInsert = System.currentTimeMillis();
		executionInsert = stopInsert - startInsert;
		log("Test sequence 2 insertion phase completed in " + executionInsert + " milliseconds.");
		startQuery = System.currentTimeMillis();
		int rootID = tree.getRoot();
		for(int i=1;i<=KEY_AMOUNT;i++){
			tree.get(i, rootID);
		}
		stopQuery = System.currentTimeMillis();
		executionQuery = stopQuery - startQuery;
		log("Test sequence 2 query phase completed in " + executionQuery + " milliseconds.");
		TEST_TWO_RUN = true;
		log("Inserted and queried " + KEY_AMOUNT + " keys with " + 
				tree.getFileManager().getNumberOfReadWrites() + " number of disk accesses.");
		log("Deleting index file for next run: "+ tree.getFileManager().deleteFile());
		log("--------------");
		return executionInsert + "/" + executionQuery;
	}
	/**
	 * Generate index from a data file
	 */
	private static long[] testSequenceThree(int keyAmount, int blockSize){
		HashSet<Integer> storedKeys = new HashSet<Integer>();
		TEST_TYPE = TEST_TYPE_THREE;
		BLOCK_SIZE = blockSize;
		KEY_AMOUNT = keyAmount;
		tree = new BPlusTree(INDEX_FILE_NAME, BLOCK_SIZE);
		log("Test sequence information\n" +
				"Block size: " + BLOCK_SIZE + " bytes\n" +
				"Node size: " + BPlusTree.getOrder() + "\n" +
				"Amount of insertions: " + KEY_AMOUNT + "\n" +
				"Data source: " + TEST_TYPE + "\n" +
				"Description: Generating index from data table stored on disk."
		);
		startInsert = System.currentTimeMillis();
		try {
			storedKeys = RecordManager.loadDatabaseIntoTree(tree, DATABASE_FILE_NAME, KEY_AMOUNT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		stopInsert = System.currentTimeMillis();
		executionInsert = stopInsert - startInsert;
		log("Test sequence 3 insertion phase completed in " + executionInsert + " milliseconds.");
		tree.getFileManager().resetReadWriteCounter(); // To only count reads for querying
		startQuery = System.currentTimeMillis();
		int rootID = tree.getRoot();
		for(int e: storedKeys){
			tree.get(e, rootID);
		}
		stopQuery = System.currentTimeMillis();
		executionQuery = stopQuery - startQuery;
		log("Test sequence 3 query phase completed in " + executionQuery + " milliseconds.");
		TEST_THREE_RUN = true;
		log("Inserted and queried " + KEY_AMOUNT + " keys with " + 
				tree.getFileManager().getNumberOfReadWrites() + " number of disk accesses.");
		TREE_LEVEL = tree.getTreeLevel();
		log("Deleting index file for next run: " + tree.getFileManager().deleteFile());
		log("--------------");
		long[] result = {executionQuery, TREE_LEVEL, tree.getFileManager().getNumberOfReadWrites()};
		return result;
	}
	/**
	 * Prints the test results for the tests that was run to stdout and log file.
	 */
	private static void printResultsFromTesting(){
		if(TEST_ONE_RUN){
			String result = "K/B";
			for(int i=0;i<BLOCK_SIZES_ARRAY.length;i++){
				result += ";" + BLOCK_SIZES_ARRAY[i];
			}
			result += "\n";
			for(int i=0;i<RESULT_ARRAY_ONE.length;i++){
				result += KEY_AMOUNT_ARRAY[i];
				for(int j=0;j<RESULT_ARRAY_ONE[i].length;j++){
					result += ";" + RESULT_ARRAY_ONE[i][j]; 
				}
				result += "\n";
			}
			log("Result for test sequence 1 (Insert all/Query all) (in ms):\n" + result);
		}
		if(TEST_TWO_RUN){
			String result = "K/B";
			for(int i=0;i<BLOCK_SIZES_ARRAY.length;i++){
				result += "    " + BLOCK_SIZES_ARRAY[i];
			}
			result += "\n";
			for(int i=0;i<RESULT_ARRAY_TWO.length;i++){
				result += KEY_AMOUNT_ARRAY[i] + " [";
				for(int j=0;j<RESULT_ARRAY_TWO[i].length;j++){
					result += " " + RESULT_ARRAY_TWO[i][j] + " "; 
				}
				result += "]\n";
			}
			log("Result for test sequence 2 (Insert all/Query all) (in ms):\n" + result);
		}
		if(TEST_THREE_RUN){
			String result = "K/B";
			for(int i=0;i<BLOCK_SIZES_ARRAY.length;i++){
				result += ";" + BLOCK_SIZES_ARRAY[i];
			}
			result += "\n";
			for(int i=0;i<RESULT_ARRAY_THREE.length;i++){
				int average = 0;
				for(int j=0;j<RESULT_ARRAY_THREE[i].length;j++){
					average+=RESULT_ARRAY_THREE[i][j];
				}
				average = average / AMOUNT_OF_RUNS;
				result += average + ";";
			}
			result += "\n";
			for(int i=0;i<TREE_LEVELS.length;i++){
				int average = 0;
				for(int j=0;j<TREE_LEVELS[i].length;j++){
					average+=TREE_LEVELS[i][j];
				}
				average = average / AMOUNT_OF_RUNS;
				result += average + ";";
			}
			result += "\n";
			for(int i=0;i<NUM_READ_WRITES.length;i++){
				int average = 0;
				for(int j=0;j<NUM_READ_WRITES[i].length;j++){
					average+=NUM_READ_WRITES[i][j];
				}
				average = average / AMOUNT_OF_RUNS;
				result += average + ";";
			}
			log("Result for test sequence 3 (Query all) (in ms):\n" + result);
		}
	}

	private static void log(String message){
		pw.println(message);
		System.out.println(message);
		System.out.flush();
	}
}
