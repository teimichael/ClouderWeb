package stu.napls.clouderweb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.cloud.storage.Blob;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import stu.napls.clouderweb.core.dictionary.StatusCode;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "web_item")
@EntityListeners(AuditingEntityListener.class)
@Data
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "path")
    private String path;

    @Column(name = "name")
    private String name;

    @Column(name = "contentType")
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "md5")
    private String md5;

    @JsonIgnore
    @Column(name = "status", columnDefinition = "integer default " + StatusCode.NORMAL)
    private int status;

    @Column(name = "createDate")
    @CreatedDate
    private Date createDate;

    @Column(name = "updateDate")
    @LastModifiedDate
    private Date updateDate;

    public Item(String path, Blob blob) {
        this.path = path;
        this.name = blob.getName();
        this.contentType = blob.getContentType();
        this.size = blob.getSize();
        this.md5 = blob.getMd5();
    }
}
