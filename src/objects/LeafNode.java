package objects;

import java.io.IOException;
import java.util.Arrays;

import data.Bytes;

/**
 * Class representing a LeafNode.
 * The LeafNode differs somewhat from the InternalNode in the insertion and get methods.
 * The LeafNode class uses the last four bytes of the header to store an eventual right leaf pointer
 * @author Emil Bergwik
 * @see Node
 */
public class LeafNode extends Node {
	/** 
	 * Constructor for LeafNode created in main-memory after splitting a LeafNode that was full.
	 * @param ID - the ID of the new node (should be the current size of the {@code RandomAccessFile}
	 */
	public LeafNode(int ID) {
		super(ID, true);
		this.keys = new byte[BPlusTree.getKeySize()*(BPlusTree.getOrder())+BPlusTree.getKeySize()];
		this.pointers = new byte[BPlusTree.getPointerSize()*(BPlusTree.getOrder()+1)+BPlusTree.getPointerSize()];
	}
	/**
	 * Constructor for LeafNode that has been read from {@code RandomAccessFile}.
	 * @param data - the byte array containing the node information, read from the RAF.
	 */
	public LeafNode(byte[] data){
		super();
		this.header = Arrays.copyOfRange(data, 0, 9);
		this.keys = new byte[BPlusTree.getKeySize()*(BPlusTree.getOrder())+BPlusTree.getKeySize()];
		this.pointers = new byte[BPlusTree.getPointerSize()*(BPlusTree.getOrder()+1)+BPlusTree.getPointerSize()];
		byte[] temporaryKeys = Arrays.copyOfRange(data, 9, 9 + BPlusTree.getKeySize()*(BPlusTree.getOrder()));
		System.arraycopy(temporaryKeys, 0, this.keys, 0, temporaryKeys.length);
		byte[] temporaryPointers = Arrays.copyOfRange(data, 
				9 + BPlusTree.getKeySize()*(BPlusTree.getOrder()), 
				9 + BPlusTree.getKeySize()*(BPlusTree.getOrder()) + 
				BPlusTree.getPointerSize()*BPlusTree.getOrder());
		System.arraycopy(temporaryPointers, 0, this.pointers, 0, temporaryPointers.length);
	}

	/**
	 * Method for inserting a <key, blockpointer> pair into the leaf node
	 * If the <key,pointer> byte array cannot hold the newly inserted <key, block pointer>,
	 * split the leaf and let the first ceil((n+1)/2) <key, pointers> stay
	 * and let the remaining floor((n+1)/2) <key, pointers> go to the new node.
	 * After that, push the right node to the parent of the old node, 
	 * set the left node's right pointer to the new node, and the new nodes 
	 * right pointer to the old nodes previous pointer.
	 * @param key - the key to insert
	 * @param pointer - the block pointer for this key
	 * @param tree - a reference to the BPlusTree
	 * @throws IOException 
	 */
	public void insert(int key, int pointer, BPlusTree tree) throws IOException{
		this.log("Inserting " + key + "," + pointer + " into LeafNode " + this.getID());
		LeafNode leftNode = this;
		LeafNode rightNode = null;
		InternalNode newInternal = null;
		byte[] keys = leftNode.getKeys();
		byte[] pointers = leftNode.getPointers();
		boolean inserted = false;
		byte[] temporaryKeys = new byte[keys.length];
		byte[] temporaryPointers = new byte[pointers.length];
		if(Bytes.bytesToInt(keys, 0) == 0){
			// The keys are empty, just put the key pointer value
			this.log("This block has no keys, inserting at the beginning.");
			Bytes.intToBytes(key, keys, 0);
			Bytes.intToBytes(pointer, pointers, 0);
			inserted = true;
		}else{
			for( int i = 0; i < keys.length-4 ; i+=4){
				if(key < Bytes.bytesToInt(keys, i)){
					// Found position for key within key range, shift keys and pointers one step to the right
					this.log("Key smaller than current value.");
					System.arraycopy(keys, i, temporaryKeys, 0, temporaryKeys.length-i);
					System.arraycopy(pointers, i, temporaryPointers, 0, temporaryPointers.length-i);
					// Then insert the new key, pointer pair
					Bytes.intToBytes(key, keys, i);
					Bytes.intToBytes(pointer, pointers, i);
					// Finally copy the shifted key, pointer pairs back
					System.arraycopy(temporaryKeys, 0, keys, i+4, temporaryKeys.length-(i+4));
					System.arraycopy(temporaryPointers, 0, pointers, i+4, temporaryPointers.length-(i+4));
					this.printKeyDiskPointers();
					inserted = true;
					break;
				}else if(key == Bytes.bytesToInt(keys, i)){
					// Found a key matching the inserted key, just update pointer
					this.log("Found matching key, updating pointer");
					Bytes.intToBytes(pointer, pointers, i);
					inserted = true;
					break;
				}
			}
			// The key is larger than any existing key, append it to the end
			if(!inserted){
				this.log("Key is larger than any other key, appending.");
				Bytes.appendInt(keys, key);
				Bytes.appendInt(pointers, pointer);
			}
		}
		if(Bytes.getLastIndex(keys) > BPlusTree.getKeySize() * BPlusTree.getOrder()){
			// The LeafNode became full after inserting, so we have to split it.
			this.log("This LeafNode is full. Splitting.");
			rightNode = new LeafNode(tree.getFileManager().getSize());
			this.log("Created new LeafNode with ID " + rightNode.getID());
			byte[] keysForRightNode = new byte[BPlusTree.getKeySize()*BPlusTree.getOrder()];
			byte[] pointersForRightNode = new byte[BPlusTree.getPointerSize()*(BPlusTree.getOrder()+1)];
			// Find the position to split keys and pointers
			int byteIndexToSplitAt = (int) (BPlusTree.getKeySize()*Math.ceil((double)(BPlusTree.getOrder()+1)/2.0));
			// Copy all keys and pointers from above positions and forward to arrays for right node
			System.arraycopy(keys, byteIndexToSplitAt, keysForRightNode, 0, keys.length-byteIndexToSplitAt);
			System.arraycopy(pointers, byteIndexToSplitAt, pointersForRightNode, 0, pointers.length-byteIndexToSplitAt);
			// Move keys and pointers to right node
			for(int i = 0; i < Bytes.getLastIndex(keysForRightNode); i+=4){
				int movingKey = Bytes.bytesToInt(keysForRightNode, i);
				int movingPointer = Bytes.bytesToInt(pointersForRightNode, i);
				this.log("Moving <" + movingKey + "," + movingPointer + "> from " 
						+ leftNode.getID() + " to " + rightNode.getID() + ".");
				rightNode.insert(movingKey, movingPointer, tree);
			}
			Bytes.clearBytesFromPosition(keys, byteIndexToSplitAt); // Removes the keys that was moved from left node
			Bytes.clearBytesFromPosition(pointers, byteIndexToSplitAt); // Removes the pointers that was moved from left node
			if(leftNode.getRightLeaf() != 0){
				rightNode.setRightLeaf(leftNode.getRightLeaf());
			}
			leftNode.setRightLeaf(rightNode.getID());
			if(leftNode.isRoot()){
				// Create a new root
				int id = tree.getFileManager().getSize();
				this.log("Split LeftNode " + this.getID() + " was previously root, creating new root InternalNode w/ ID " + id);
				newInternal = new InternalNode(id);
				tree.setRoot(newInternal.getID(), 0);
				newInternal.setSmallestPointer(leftNode.getID());
				newInternal.insert(Bytes.bytesToInt(rightNode.getKeys(), 0), rightNode.getID(), tree);
				tree.getFileManager().write(newInternal.toBytes());
			}else{
				// Push the new right node ID to parent
				newInternal = (InternalNode) leftNode.parent;
				this.log("Pushing up right node ID " + rightNode.getID() + " to parent ID " + newInternal.getID());
				newInternal.insert(Bytes.bytesToInt(rightNode.getKeys(), 0), rightNode.getID(), tree);
				tree.getFileManager().write(newInternal.toBytes(), newInternal.getID());
			}			
		}
		if(rightNode != null) tree.getFileManager().write(rightNode.toBytes(), rightNode.getID());
		leftNode.setKeys(keys);
		leftNode.setPointers(pointers);
		leftNode.printKeyDiskPointers();
		tree.getFileManager().write(leftNode.toBytes(), leftNode.getID());
	}

	public int get(int key){
		byte[] keys = this.getKeys();
		byte[] pointers = this.getPointers();
		for(int i = 0; i < keys.length; i+=4){
			if(Bytes.bytesToInt(keys, i) == key){
				return Bytes.bytesToInt(pointers, i);
			}
		}
		return 0;
	}

	public void printKeyDiskPointers(){
		this.log("----- LeafNode " + this.getID() + " -----");
		byte[] keys = this.getKeys();
		byte[] pointers = this.getPointers();
		String result = "Keys:\n[ ";
		for(int i = 0; i < keys.length-4; i+=4){
			 result += Bytes.bytesToInt(keys, i) + " ";
		}
		result += "]\nPointers:\n[ ";
		for(int i = 0; i < pointers.length-8; i+=4){
			result += Bytes.bytesToInt(pointers, i) + " ";
		}
		if(this.getRightLeaf() != 0){
			result += this.getRightLeaf() + " ";
		}
		result += "]";
		this.log(result);
	}

	public int getRightLeaf(){
		return Bytes.bytesToInt(this.header, this.header.length-4);
	}
	/**
	 * The right LeafNode pointer is always stored at last 4 bytes in data
	 * @param rightLeafID
	 */
	public void setRightLeaf(int rightLeafID){
		Bytes.intToBytes(rightLeafID, this.header, this.header.length-4);
	}
}