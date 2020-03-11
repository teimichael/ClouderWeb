package stu.napls.clouderweb.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    String storeImage(MultipartFile image, String path, String name) throws IOException;

}
