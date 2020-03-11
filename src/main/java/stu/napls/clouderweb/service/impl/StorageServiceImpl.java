package stu.napls.clouderweb.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import stu.napls.clouderweb.config.property.StaticServer;
import stu.napls.clouderweb.core.exception.Assert;
import stu.napls.clouderweb.service.StorageService;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @Author Tei Michael
 * @Date 1/13/2020
 */
@Service("storageService")
public class StorageServiceImpl implements StorageService {

    @Resource
    private StaticServer staticServer;


    @Override
    public String storeImage(MultipartFile image, String path, String name) throws IOException {
        // Create path
        if (!Files.exists(Paths.get(staticServer.getRootPath() + path))) {
            Files.createDirectories(Paths.get(staticServer.getRootPath() + path));
        }
        String contentType = image.getContentType();
        Assert.notNull(contentType, "Invalid type of image.");
        switch (contentType) {
            case "image/png": {
                contentType = "png";
                break;
            }
            case "image/jpeg": {
                contentType = "jpg";
                break;
            }
            case "image/gif": {
                contentType = "gif";
                break;
            }
        }
        name = name + "." + contentType;
        ImageIO.write(ImageIO.read(image.getInputStream()), contentType, new File(staticServer.getRootPath() + path + name));
        return name;
    }

}
