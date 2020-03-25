package stu.napls.clouderweb.service.impl;

import org.springframework.stereotype.Service;
import stu.napls.clouderweb.core.dictionary.StatusCode;
import stu.napls.clouderweb.model.Folder;
import stu.napls.clouderweb.repository.FolderRepository;
import stu.napls.clouderweb.service.FolderService;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service("folderService")
public class FolderServiceImpl implements FolderService {

    @Resource
    private FolderRepository folderRepository;

    @Override
    public Folder findById(long id) {
        Folder record = null;
        Optional<Folder> result = folderRepository.findById(id);
        if (result.isPresent()) {
            record = result.get();
        }
        return record;
    }

    @Override
    public Folder update(Folder folder) {
        return folderRepository.save(folder);
    }

    @Override
    public List<Folder> saveAll(List<Folder> folders) {
        return folderRepository.saveAll(folders);
    }

    @Override
    public void delete(Folder folder) {
        folderRepository.delete(folder);
    }

    @Transactional
    @Override
    public void deleteByIds(List<Long> ids) {
        folderRepository.deleteByIdIn(ids);
    }

    @Override
    public Folder findByParentFolderIdAndName(long parentFolderId, String name) {
        return folderRepository.findByParentFolderIdAndNameAndStatus(parentFolderId, name, StatusCode.NORMAL);
    }

    @Override
    public Folder findByPathAndDepositoryId(String path, long depositoryId) {
        return folderRepository.findByPathAndDepositoryIdAndStatus(path, depositoryId, StatusCode.NORMAL);
    }

    @Override
    public List<Folder> findByParentFolderId(long parentFolderId) {
        return folderRepository.findByParentFolderIdAndStatus(parentFolderId, StatusCode.NORMAL);
    }

}
