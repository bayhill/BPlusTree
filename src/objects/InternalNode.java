package objects;

import java.io.IOException;
import java.util.Arrays;

import data.Bytes;

/**
 * An InternalNode can only contain keys and child pointers, never disk pointers.
 * It can either contain pointers to other internal nodes or to leaf nodes.
 * @author Emil Bergwik
 * @see Node
 */
public class InternalNode extends Node {

	/** 
	 * Constructor for InternalNode created in main-memory after either splitting an InternalNode or splitting
	 * a LeafNode that was previously root.
	 * @param ID - the ID of the new node (should be the current size of the {@code RandomAccessFile}
	 */
	public InternalNode(int ID){
		super(ID, false);
		this.keys = new byte[BPlusTree.getKeySize()*BPlusTree.getOrder()+BPlusTree.getKeySize()];
		this.pointers = new byte[BPlusTree.getPointerSize()*(BPlusTree.getOrder()+1)+BPlusTree.getPointerSize()];
	}
	/**
	 * Constructor for InternalNode that has been read from {@code RandomAccessFile}.
	 * @param data - the byte array containing the node information, read from the RAF.
	 */
	public InternalNode(byte[] data){
		super();
		this.header = Arrays.copyOfRange(data, 0, 9);
		this.keys = new byte[BPlusTree.getKeySize()*(BPlusTree.getOrder())+BPlusTree.getKeySize()];
		this.pointers = new byte[BPlusTree.getPointerSize()*(BPlusTree.getOrder()+1)+BPlusTree.getPointerSize()];
		byte[] temporaryKeys = Arrays.copyOfRange(data, 9, 9 + BPlusTree.getKeySize()*(BPlusTree.getOrder()));
		System.arraycopy(temporaryKeys, 0, this.keys, 0, temporaryKeys.length);
		byte[] temporaryPointers = Arrays.copyOfRange(data, 
				9 + BPlusTree.getKeySize()*(BPlusTree.getOrder()), 
				9 + BPlusTree.getKeySize()*(BPlusTree.getOrder()) + 
				BPlusTree.getPointerSize()*(BPlusTree.getOrder()+1));
		System.arraycopy(temporaryPointers, 0, this.pointers, 0, temporaryPointers.length);
	}

	/**
	 * Method for insertig a <key, childpointer> pair into an internal node
	 * If the array overflows (i.e its amount of keys is larger than the order of the tree),
	 * split the internal node and let the first ceil((n+2)/2) pointers stay in old node,
	 * and let the remaining floor((n+2)/2) pointers move to new node.
	 * Let the ceil(n/2) keys stay in old node and move floor(n/2) keys to new one.
	 * The middle key is pushed recursively up in the parent along with a pointer to the new node.
	 * In this implementation, the childNode that has been pushed up is always the new right node resulting from 
	 * a split.
	 * @param key - the key to insert
	 * @param childNode - the pointer to the newly created child node
	 * @param tree - a reference to the BPlusTree
	 * @throws IOException 
	 */
	public InternalNode insert(int key, int pointer, BPlusTree tree) throws IOException{
		this.log("Inserting " + key + "," + pointer + " into InternalNode " + this.getID());
		InternalNode leftNode = this;
		InternalNode rightNode = null;
		InternalNode newInternal = null;
		byte[] keys = leftNode.getKeys();
		byte[] pointers = leftNode.getPointers();
		boolean inserted = false;
		byte[] temporaryKeys = new byte[keys.length];
		byte[] temporaryPointers = new byte[pointers.length];
		// Whether or not the Node is full, insert the key (possibly in the extra key, pointer position)
		if(Bytes.bytesToInt(keys, 0) == 0){
			// The keys are empty, just put the key pointer value
			this.log("Key set are empty. Inserting at the beginning.");
			Bytes.intToBytes(key, keys, 0);
			Bytes.intToBytes(pointer, pointers, 4);
		}else{
			for( int i = 0; i < keys.length-4 ; i+=4){
				if(key < Bytes.bytesToInt(keys, i)){
					// Found position for key within key range, shift keys and pointers one step to the right
					this.log("Key smaller than current value.");
					System.arraycopy(keys, i, temporaryKeys, 0, temporaryKeys.length-i);
					System.arraycopy(pointers, i+4, temporaryPointers, 0, temporaryPointers.length-(i+4));
					// Then insert the new key, pointer pair
					Bytes.intToBytes(key, keys, i);
					Bytes.intToBytes(pointer, pointers, i+4);
					// Finally copy the shifted key, pointer pairs back
					System.arraycopy(temporaryKeys, 0, keys, i+4, temporaryKeys.length-(i+4));
					System.arraycopy(temporaryPointers, 0, pointers, i+8, temporaryPointers.length-(i+8));
					inserted = true;
					break;
				}else if(key == Bytes.bytesToInt(keys, i) && !inserted){
					// Found a key matching the inserted key, just update pointer
					this.log("Found matching key, updating pointer");
					Bytes.intToBytes(pointer, pointers, i+4);
					this.setPointers(pointers);
					inserted = true;
					break;
				}
			}
			// The key is larger than any existing key in InternalNode, append it to the end
			if(!inserted){
				this.log("Key is larger than any other key, appending.");
				Bytes.appendInt(keys, key);
				Bytes.appendInt(pointers, pointer);
			}
			this.keys = new byte[BPlusTree.getKeySize()*(BPlusTree.getOrder())+BPlusTree.getKeySize()];
			if(Bytes.getLastIndex(keys) > BPlusTree.getKeySize() * BPlusTree.getOrder()){
				// Full, so we have to split.
				this.log("This InternalNode is full. Splitting.");
				rightNode = new InternalNode(tree.getFileManager().getSize());
				this.log("Created new InternalNode with ID " + rightNode.getID());
				byte[] keysForRightNode = new byte[BPlusTree.getKeySize()*BPlusTree.getOrder()];
				byte[] pointersForRightNode = new byte[BPlusTree.getPointerSize()*(BPlusTree.getOrder()+1)];
				// Find the position to split keys and pointers
				int keyByteIndexToSplitAt = (int) (BPlusTree.getKeySize()*
						Math.ceil(((double)(BPlusTree.getOrder()))/2.0));
				int pointerByteIndexToSplitAt = (int) (BPlusTree.getPointerSize()*
						Math.ceil(((double)(BPlusTree.getOrder()+2))/2.0));
				// Copy all keys and pointers from above positions and forward to arrays for right node
				System.arraycopy(keys, keyByteIndexToSplitAt, keysForRightNode, 0, keys.length-keyByteIndexToSplitAt);
				System.arraycopy(pointers, pointerByteIndexToSplitAt, pointersForRightNode, 0, pointers.length-pointerByteIndexToSplitAt);
				// Move keys and pointers to right node
				rightNode.setSmallestPointer(Bytes.bytesToInt(pointersForRightNode,0));
				for(int i = 4; i < Bytes.getLastIndex(keysForRightNode); i+=4){
					int movingKey = Bytes.bytesToInt(keysForRightNode, i);
					int movingPointer = Bytes.bytesToInt(pointersForRightNode, i);
					this.log("Moving <" + movingKey + "," + movingPointer + "> from " 
							+ leftNode.getID() + " to " + rightNode.getID() + ".");
					rightNode.insert(movingKey, movingPointer, tree);
				}
				Bytes.clearBytesFromPosition(keys, keyByteIndexToSplitAt); // Removes the keys that was moved from left node
				Bytes.clearBytesFromPosition(pointers, pointerByteIndexToSplitAt); // Removes the pointers that was moved from left node
				tree.getFileManager().write(rightNode.toBytes(), rightNode.getID());
				if(leftNode.isRoot()){
					// Create a new root
					int id = tree.getFileManager().getSize();
					this.log("Split InternalNode " + this.getID() + " was previously root, creating new root InternalNode w/ ID " + id);
					newInternal = new InternalNode(id);
					tree.setRoot(newInternal.getID(), 0);
					newInternal.setSmallestPointer(leftNode.getID());
					newInternal.insert(Bytes.bytesToInt(keysForRightNode,0), rightNode.getID(), tree);
					tree.getFileManager().write(newInternal.toBytes());
				}else{
					// Push the new right node ID to parent
					newInternal = (InternalNode) leftNode.parent;
					this.log("Pushing right node ID " + rightNode.getID() + " to parent ID" + parent.getID());
					newInternal.insert(Bytes.bytesToInt(keysForRightNode,0), rightNode.getID(), tree);
					tree.getFileManager().write(newInternal.toBytes(), newInternal.getID());
				}
			}
			if(rightNode != null) tree.getFileManager().write(rightNode.toBytes(), rightNode.getID());
			leftNode.setKeys(keys);
			leftNode.setPointers(pointers);
			leftNode.printKeyDiskPointers();
			tree.getFileManager().write(leftNode.toBytes(), leftNode.getID());
		}
		return this;
	}
/**
 * Sets the smallest pointer reachable via this InternalNode.
 * @param smallestPointer - the pointer to set as smallest pointer
 */
	public void setSmallestPointer(int smallestPointer){
		this.log("Setting smallest pointer: " + smallestPointer + " in InternalNode " + this.getID());
		byte[] pointers = this.getPointers();
		byte[] temporaryPointers = new byte[pointers.length];
		System.arraycopy(pointers, 0, temporaryPointers, 0, temporaryPointers.length);
		Bytes.intToBytes(smallestPointer, pointers, 0);
		System.arraycopy(temporaryPointers, 0, pointers, 4, temporaryPointers.length-4);
		this.setPointers(pointers);
	}

	/** Method for getting the child node (or subtree) that might contain the key.
	 *  @return The child node that contains the key.
	 */
	public int get(int key){
		byte[] keys = this.getKeys();
		byte[] pointers = this.getPointers();
		if(key < Bytes.bytesToInt(keys, 0)){
			this.log(key + " is smaller than smallest key " + Bytes.bytesToInt(keys, 0));
			return Bytes.bytesToInt(pointers, 0);
		}else if(key >= Bytes.bytesToInt(keys, Bytes.getLastIndex(keys)-4)){
			this.log(key + " is greater than greatest key " + Bytes.bytesToInt(keys, Bytes.getLastIndex(keys)-4));
			return Bytes.bytesToInt(pointers, Bytes.getLastIndex(pointers)-4);
		}else{
			for(int i = 0; i <= keys.length-4; i+=4){
				if(key >= Bytes.bytesToInt(keys, i) && key < Bytes.bytesToInt(keys, i+4)){
					return Bytes.bytesToInt(pointers, i+4);
				}
			}
		}
		return 0;
	}

	public void printKeyDiskPointers(){
		this.log("----- InternalNode " + this.getID() + " -----");
		byte[] keys = this.getKeys();
		byte[] pointers = this.getPointers();
		String result = "Keys:\n[ ";
		for(int i = 0; i < keys.length-4; i+=4){
			result += Bytes.bytesToInt(keys, i) + " ";
		}
		result += "]\nPointers:\n[ ";
		for(int i = 0; i < pointers.length-4; i+=4){
			result += Bytes.bytesToInt(pointers, i) + " ";
		}
		result += "]";
		this.log(result);
	}
}
