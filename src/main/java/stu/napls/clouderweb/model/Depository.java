package stu.napls.clouderweb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import stu.napls.clouderweb.core.dictionary.StatusCode;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "web_depository")
@EntityListeners(AuditingEntityListener.class)
@Data
public class Depository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "Unit: byte")
    @Column(name = "capacity")
    private Long capacity;

    @ApiModelProperty(value = "Unit: byte")
    @Column(name = "used_space")
    private Long usedSpace;

    @OneToMany(fetch = FetchType.EAGER)
    private Set<Item> items;

    @JsonIgnore
    @Column(name = "status", columnDefinition = "integer default " + StatusCode.NORMAL)
    private int status;

    @Column(name = "createDate")
    @CreatedDate
    private Date createDate;

    @Column(name = "updateDate")
    @LastModifiedDate
    private Date updateDate;

}
