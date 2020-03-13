package stu.napls.clouderweb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import stu.napls.clouderweb.core.dictionary.StatusCode;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "web_foler")
@EntityListeners(AuditingEntityListener.class)
@Data
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parentFolderId")
    private Long parentFolderId;

    @Column(name = "path")
    private String path;

    @Column(name = "userId")
    private Long userId;

    @JsonIgnore
    @Column(name = "status", columnDefinition = "integer default " + StatusCode.NORMAL)
    private int status;

    @JsonIgnore
    @Column(name = "createDate")
    @CreatedDate
    private Date createDate;

    @JsonIgnore
    @Column(name = "updateDate")
    @LastModifiedDate
    private Date updateDate;
}
