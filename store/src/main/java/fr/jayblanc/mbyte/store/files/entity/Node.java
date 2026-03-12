package fr.jayblanc.mbyte.store.files.entity;

import fr.jayblanc.mbyte.store.files.FileService;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

@Entity
@NamedQueries({
    @NamedQuery(name = "Node.findAllChildren", query = "SELECT n FROM Node n WHERE n.owner = :owner AND n.parent = :parent"),
    @NamedQuery(name = "Node.findAll", query = "SELECT n FROM Node n WHERE n.owner = :owner"),
    @NamedQuery(name = "Node.findChildrenForName", query = "SELECT n FROM Node n WHERE n.owner = :owner AND n.parent = :parent AND n.name = :name"),
    @NamedQuery(name = "Node.countChildren", query = "SELECT count(n) FROM Node n WHERE n.owner = :owner AND n.parent = :parent"),
    @NamedQuery(name = "Node.findByOwnerAndId", query = "SELECT n FROM Node n WHERE n.owner = :owner AND n.id = :id"),
})
@Table(indexes = {
    @Index(name = "owner_parent", columnList = "owner, parent"),
    @Index(name = "owner_parent_name", columnList = "owner, parent, name"),
})
public class Node implements Comparable<Node>, Serializable {

    @Enumerated(EnumType.STRING)
    private Type type;
    @Id
    @Column(length = 50)
    private String id;
    @Column(length = 50)
    private String parent;
    @Version
    private long version;
    private String name;
    @Column(length = 250)
    private String owner;
    @Column(length = 50)
    private String mimetype;
    private long size;
    private long creation;
    private long modification;
    private String content;

    public Node() {
        this.creation = this.modification = System.currentTimeMillis();
        this.size = 0;
    }

    public Node(Type type, String parent, String id, String name) {
        this();
        this.type = type;
        this.parent = parent;
        this.id = id;
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCreation() {
        return creation;
    }

    public void setCreation(long creation) {
        this.creation = creation;
    }

    public long getModification() {
        return modification;
    }

    public void setModification(long modification) {
        this.modification = modification;
    }

    public boolean isRoot() {
        return this.id.equals(FileService.ROOT_NODE_ID);
    }

    public boolean isFolder() {
        return this.type.equals(Type.TREE);
    }

    public enum Type {
        TREE,
        BLOB
    }

    @Override
    public int compareTo(Node o) {
        if (this.getType().equals(o.getType())) {
            return this.getName().compareTo(o.getName());
        } else if (this.getType().equals(Type.TREE)){
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return version == node.version && size == node.size && creation == node.creation && modification == node.modification && type == node.type && Objects.equals(id, node.id) && Objects.equals(parent, node.parent) && Objects.equals(name, node.name) && Objects.equals(mimetype, node.mimetype) && Objects.equals(content, node.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id, parent, version, name, mimetype, size, creation, modification, content);
    }

    @Override
    public String toString() {
        return "Node{" +
                "type=" + type +
                ", id='" + id + '\'' +
                ", version=" + version +
                ", parent=" + parent +
                ", name='" + name + '\'' +
                ", mimetype='" + mimetype + '\'' +
                ", size=" + size +
                ", creation=" + creation +
                ", modification=" + modification +
                ", content='" + content + '\'' +
                '}';
    }

    public static class NameComparatorAsc implements Comparator<Node> {
        @Override
        public int compare(Node o1, Node o2) {
            if ( o1.isFolder() && !o2.isFolder() ) {
                return -1;
            }
            if ( !o1.isFolder() && o2.isFolder() ) {
                return 1;
            }
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static class NameComparatorDesc implements Comparator<Node> {
        @Override
        public int compare(Node o1, Node o2) {
            if ( o1.isFolder() && !o2.isFolder() ) {
                return -1;
            }
            if ( !o1.isFolder() && o2.isFolder() ) {
                return 1;
            }
            return o2.getName().compareTo(o1.getName());
        }
    }
}
