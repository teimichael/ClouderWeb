package stu.napls.clouderweb.service.impl;

import org.springframework.stereotype.Service;
import stu.napls.clouderweb.model.Depository;
import stu.napls.clouderweb.repository.DepositoryRepository;
import stu.napls.clouderweb.service.DepositoryService;

import javax.annotation.Resource;

@Service("depositoryService")
public class DepositoryServiceImpl implements DepositoryService {

    @Resource
    private DepositoryRepository depositoryRepository;

    @Override
    public Depository update(Depository depository) {
        return depositoryRepository.save(depository);
    }
}
