package tree;

public class TreeNode {
    private final String label;
    private TreeNode left;
    private TreeNode right;
    private TreeNode parent;
    private java.util.List<String> universe;
    private java.util.List<String> edgeK;
    
    public TreeNode(String label) {
        this.label = label;
    }
    
    public java.util.List<String> getUniverse() {
        return universe;
    }
    
    public void setUniverse(java.util.List<String> universe) {
        this.universe = universe;
    }
    
    public java.util.List<String> getEdgeK() {
        return edgeK;
    }
    
    public void setEdgeK(java.util.List<String> edgeK) {
        this.edgeK = edgeK;
    }

    public String getLabel() {
        return label;
    }
    
    public TreeNode getLeft() {
        return left;
    }
    
    public void setLeft(TreeNode left) {
        this.left = left;
        if (left != null) {
            left.parent = this;
        }
    }
    
    public TreeNode getRight() {
        return right;
    }
    
    public void setRight(TreeNode right) {
        this.right = right;
        if (right != null) {
            right.parent = this;
        }
    }
    
    public TreeNode getParent() {
        return parent;
    }
    
    public boolean isLeaf() {
        return left == null && right == null;
    }
    
    public boolean isRoot() {
        return parent == null;
    }
    
    public TreeNode leftChild() {
        return left;
    }
    
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
