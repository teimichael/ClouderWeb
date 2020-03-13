package stu.napls.clouderweb.service.impl;

import org.springframework.stereotype.Service;
import stu.napls.clouderweb.model.Folder;
import stu.napls.clouderweb.repository.FolderRepository;
import stu.napls.clouderweb.service.FolderService;

import javax.annotation.Resource;
import java.util.List;

@Service("folderService")
public class FolderServiceImpl implements FolderService {

    @Resource
    private FolderRepository folderRepository;

    @Override
    public Folder create(Folder folder) {
        return folderRepository.save(folder);
    }

    @Override
    public Folder findByUserIdAndPath(long userId, String path) {
        return folderRepository.findByUserIdAndPath(userId, path);
    }

    @Override
    public List<Folder> findByUserIdAndParentFolderId(long userId, long parentFolderId) {
        return folderRepository.findByUserIdAndParentFolderId(userId, parentFolderId);
    }
}
