package stu.napls.clouderweb.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import stu.napls.clouderweb.core.dictionary.StatusCode;
import stu.napls.clouderweb.model.Item;
import stu.napls.clouderweb.repository.ItemRepository;
import stu.napls.clouderweb.service.ItemService;

import javax.annotation.Resource;

@Service("itemService")
public class ItemServiceImpl implements ItemService {

    @Resource
    private ItemRepository itemRepository;

    @Override
    public Item update(Item item) {
        return itemRepository.save(item);
    }

    @Override
    public void delete(Item item) {
        itemRepository.delete(item);
    }

    @Override
    public Item findByPathAndName(String path, String name) {
        return itemRepository.findByPathAndName(path, name);
    }

    @Override
    public Page<Item> findAllByDepositoryId(long depositoryId, Pageable pageable) {
        return itemRepository.findByDepositoryIdAndStatus(depositoryId, StatusCode.NORMAL, pageable);
    }
}
