package stu.napls.clouderweb.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviewVO {
    private int type;

    private String contentType;

    private String content;

}
