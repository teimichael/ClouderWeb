package stu.napls.clouderweb.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import stu.napls.clouderweb.core.dictionary.StatusCode;
import stu.napls.clouderweb.model.Item;
import stu.napls.clouderweb.repository.ItemRepository;
import stu.napls.clouderweb.service.ItemService;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service("itemService")
public class ItemServiceImpl implements ItemService {

    @Resource
    private ItemRepository itemRepository;

    @Override
    public Item findById(long id) {
        Item record = null;
        Optional<Item> result = itemRepository.findById(id);
        if (result.isPresent()) {
            record = result.get();
        }
        return record;
    }

    @Override
    public Item update(Item item) {
        return itemRepository.save(item);
    }

    @Override
    public List<Item> saveAll(List<Item> items) {
        return itemRepository.saveAll(items);
    }

    @Override
    public void delete(Item item) {
        itemRepository.delete(item);
    }

    @Transactional
    @Override
    public void deleteByIds(List<Long> ids) {
        itemRepository.deleteByIdIn(ids);
    }

    @Override
    public Item findByFolderIdAndName(long folderId, String name) {
        return itemRepository.findByFolderIdAndNameAndStatus(folderId, name, StatusCode.NORMAL);
    }

    @Override
    public List<Item> findAllByFolderId(long folderId) {
        return itemRepository.findByFolderIdAndStatus(folderId, StatusCode.NORMAL);
    }

    @Override
    public Page<Item> findAllByDepositoryIdAndType(long depositoryId, int type, Pageable pageable) {
        return itemRepository.findByDepositoryIdAndTypeAndStatus(depositoryId, type, StatusCode.NORMAL, pageable);
    }

    @Override
    public Page<Item> findAllByFolderId(long folderId, Pageable pageable) {
        return itemRepository.findByFolderIdAndStatus(folderId, StatusCode.NORMAL, pageable);
    }
}
