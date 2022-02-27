package com.brimworks.serde;

/**
 * A linked list of path elements used to describe locations.
 */
public class SerdePath {
    private SerdePath parent;
    private int lineNumber;
    private int columnNumber;
    private long byteOffset;
    private int index;
    private String key;

    private SerdePath(SerdePath parent, int lineNumber, int columnNumber, long byteOffset, int index, String key) {
        this.parent = parent;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.byteOffset = byteOffset;
        this.index = index;
        this.key = key;
    }

    public static SerdePath ofObjectKey(String key, SerdePath parent, int lineNumber, int columnNumber, long byteOffset) {
        assert null != key;
        return new SerdePath(parent, lineNumber, columnNumber, byteOffset, -1, key);
    }

    public static SerdePath ofArrayIndex(int index, SerdePath parent, int lineNumber, int columnNumber, long byteOffset) {
        assert index >= 0;
        return new SerdePath(parent, lineNumber, columnNumber, byteOffset, index, null);
    }

    public boolean isObjectKey() {
        return null != key;
    }

    public boolean isArrayIndex() {
        return null == key;
    }

    /**
     * @return the object key or null if this is not an object key.
     */
    public String getObjectKey() {
        return key;
    }

    /**
     * @return the array index or -1 if this is not an array index.
     */
    public int getArrayIndex() {
        return index;
    }

    /**
     * 1 is the first line, may be 0 if no textual input.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * 1 is the first column of a line, may be 0 if no textual input.
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * 0 based byte offset.
     */
    public long getByteOffset() {
        return byteOffset;
    }

    /**
     * @return the parent of the current path or null if no parent
     */
    public SerdePath getParent() {
        return parent;
    }

    private void toString(StringBuilder sb) {
        if (null != parent) {
            parent.toString(sb);
            sb.append("\n");
        }
        sb.append("\tat ");
        if (isObjectKey()) {
            sb.append('"').append(key).append('"');
        } else {
            sb.append("[").append(index).append("]");
        }
        sb.append("(line ");
        sb.append(lineNumber);
        sb.append(" column ");
        sb.append(columnNumber);
        sb.append(" offset ");
        sb.append(byteOffset);
        sb.append(")");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }
}
