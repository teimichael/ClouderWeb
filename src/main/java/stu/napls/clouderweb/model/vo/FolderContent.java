package stu.napls.clouderweb.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import stu.napls.clouderweb.model.Folder;
import stu.napls.clouderweb.model.Item;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderContent {

    private List<Folder> folders;

    private Page<Item> items;

}
