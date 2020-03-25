package stu.napls.clouderweb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import stu.napls.clouderweb.core.dictionary.StatusCode;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "web_foler")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "parentFolderId")
    private Long parentFolderId;

    @Column(name = "path")
    private String path;

    @Column(name = "name")
    private String name;

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

    public Folder(long depositoryId, String name, String path, long parentFolderId) {
        this.uuid = UUID.randomUUID().toString().replaceAll("-", "");
        this.depositoryId = depositoryId;
        this.name = name;
        this.path = path;
        this.parentFolderId = parentFolderId;
    }
}
