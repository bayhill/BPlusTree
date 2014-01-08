package data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Class for simulating an index file. An FileManager can read and write to blocks
 * within the FileManager. the FileManager stores the number of blocks in the file, the block size
 * and the random access file it writes and reads to.
 * It contains methods for writing and reading from a specific position within the RAF.
 * @author Emil Bergwik
 *
 */
public class FileManager {

	private RandomAccessFile file;
	private File inputFile;
	private FileChannel fc;
	private ByteBuffer bb;
	private final int blockSize;  // Size of one block
	private int size = 0; // Number of blocks in the file
    private long numReadWrites = 0; // Number of reads and writes performed on RAF
	/**
	 * Constructor for the FileManager. Given a file name and a block size, either opens an existing file
	 * (if the file exists) or creates a new one, in R/W mode. 
	 * @param name - the file name to open
	 * @param blockSize - the size of each block in the file
	 * @throws IOException
	 */
	public FileManager(String name, int blockSize){
		this.blockSize = blockSize;
		this.numReadWrites = 0;
		this.inputFile = new File(name);
		try {
			this.file = new RandomAccessFile(inputFile, "rw");
			this.fc = file.getChannel();
			this.bb = ByteBuffer.allocate(blockSize);
			if (inputFile.exists()){
				this.size  = (int) (this.inputFile.length() / blockSize);
				if (this.inputFile.length() % blockSize != 0){
					this.file.setLength(size*blockSize);
				}
			}else{
				this.size  = 0;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException ie){
			ie.printStackTrace();
		}
	}	

	/** 
	 * Method for writing a block to the FileManager, given a position to write it to.
	 * @param block - the Node to write to the FileManager
	 * @param position - the position to write it to
	 * @throws IOException If an I/O error occurs in the process of writing to the FileManager
	 */
	public void write(byte[] bytes, int position) throws IOException {
		if(position < 0 || position > size){
			throw new IndexOutOfBoundsException("Position Index out of bounds when writing to block " + position + "\n"
					+ "Size of file is " + size);
		}else{
			bb = ByteBuffer.allocate(blockSize);
			bb.put(bytes);
			bb.rewind();
			fc.write(bb, position*blockSize);
			this.numReadWrites++;
			if(position == size) size++;
		}
	}
	/**
	 * Method for writing a block to the end of the FileManager.
	 * @param bytes - the byte representation of the Node to write to the FileManager
	 * @return the new size of the FileManager (after writing the block).
	 * @throws IOException if either the seek or the writing of the block to the file fails.
	 */
	public int write(byte[] bytes)throws IOException{
		bb = ByteBuffer.allocate(blockSize);
		bb.put(bytes);
		bb.rewind();
		fc.write(bb, size*blockSize);
		this.numReadWrites++;
		return ++size;
	}
	/**
	 * Method for reading a block given a position within the file
	 * @param blockPosition - the position of the block in the file
	 * @return the block that found at that position
	 * @throws IOException if the total amount of bytes read into the byte buffer does not match the block size
	 */
	public byte[] read(int blockPosition) throws IOException{
		if(blockPosition < 0 || blockPosition > size){
			throw new IndexOutOfBoundsException();
		}

		bb = ByteBuffer.allocate(blockSize);
		fc.read(bb, blockPosition*blockSize);
		this.numReadWrites++;
		return bb.array();		
	}

	public int getBlockSize(){
		return blockSize;
	}

	public int getSize(){
		return size;
	}
	public long getNumberOfReadWrites(){
		return this.numReadWrites;
	}
	
	public void resetReadWriteCounter(){
		this.numReadWrites = 0;
	}
	
	/**
	 * Deletes the underlying file that the RAF accesses, after it closes the file.
	 * @return true if the file was successfully deleted.
	 */
	public boolean deleteFile(){
		try {
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return inputFile.delete();
	}
	/**
	 * Method for closing the random access file
	 * @throws IOException
	 */
	public void close() throws IOException {
		file.close();
	}
}
