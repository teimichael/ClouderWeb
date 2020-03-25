package stu.napls.clouderweb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.cloud.storage.Blob;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import stu.napls.clouderweb.core.dictionary.StatusCode;
import stu.napls.clouderweb.util.FileChecker;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "web_item")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "folderId")
    private Long folderId;

    @Column(name = "name")
    private String name;

    @Column(name = "suffix")
    private String suffix;

    @Column(name = "type")
    private int type;

    @Column(name = "contentType")
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "md5")
    private String md5;

    @Column(name = "depositoryId")
    private Long depositoryId;

    @JsonIgnore
    @Column(name = "status", columnDefinition = "integer default " + StatusCode.NORMAL)
    private int status;

    @Column(name = "createDate")
    @CreatedDate
    private Date createDate;

    @Column(name = "updateDate")
    @LastModifiedDate
    private Date updateDate;

    public Item(long depositoryId, long folderId, String name, Blob blob) {
        this.uuid = UUID.randomUUID().toString().replaceAll("-", "");
        this.depositoryId = depositoryId;
        this.folderId = folderId;
        this.name = name;
        this.suffix = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1).toLowerCase() : "";
        this.type = FileChecker.getItemType(this.suffix);
        this.contentType = blob.getContentType();
        this.size = blob.getSize();
        this.md5 = blob.getMd5();
    }

}
