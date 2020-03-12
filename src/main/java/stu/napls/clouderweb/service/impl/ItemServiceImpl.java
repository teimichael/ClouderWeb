package stu.napls.clouderweb.service.impl;

import org.springframework.stereotype.Service;
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
}
