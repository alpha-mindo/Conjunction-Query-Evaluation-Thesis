package tree;

/**
 * Represents a node in the join tree.
 * Leaf nodes correspond to base relations.
 * Internal nodes correspond to join operations.
 */
public class TreeNode {
    private final String label;
    private TreeNode left;
    private TreeNode right;
    private TreeNode parent;
    
    /**
     * Creates a new tree node with the given label.
     * @param label The node identifier (typically a relation name for leaves)
     */
    public TreeNode(String label) {
        this.label = label;
    }
    
    /**
     * Gets the label of this node.
     * @return The node label
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Gets the left child of this node.
     * @return Left child node, or null if none
     */
    public TreeNode getLeft() {
        return left;
    }
    
    /**
     * Sets the left child of this node.
     * @param left The left child node
     */
    public void setLeft(TreeNode left) {
        this.left = left;
        if (left != null) {
            left.parent = this;
        }
    }
    
    /**
     * Gets the right child of this node.
     * @return Right child node, or null if none
     */
    public TreeNode getRight() {
        return right;
    }
    
    /**
     * Sets the right child of this node.
     * @param right The right child node
     */
    public void setRight(TreeNode right) {
        this.right = right;
        if (right != null) {
            right.parent = this;
        }
    }
    
    /**
     * Gets the parent of this node.
     * @return Parent node, or null if this is the root
     */
    public TreeNode getParent() {
        return parent;
    }
    
    /**
     * Checks if this node is a leaf (has no children).
     * @return true if this is a leaf node
     */
    public boolean isLeaf() {
        return left == null && right == null;
    }
    
    /**
     * Checks if this node is the root (has no parent).
     * @return true if this is the root node
     */
    public boolean isRoot() {
        return parent == null;
    }
    
    /**
     * Gets the left child (alias for getLeft).
     * @return Left child node
     */
    public TreeNode leftChild() {
        return left;
    }
    
    /**
     * Gets the right child (alias for getRight).
     * @return Right child node
     */
    public TreeNode rightChild() {
        return right;
    }
    
    @Override
    public String toString() {
        return "TreeNode{" + label + 
               (isLeaf() ? " (leaf)" : "") + 
               (isRoot() ? " (root)" : "") + "}";
    }
}
