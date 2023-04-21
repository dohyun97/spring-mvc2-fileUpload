package hello.upload.controller;

import hello.upload.domain.Item;
import hello.upload.domain.ItemRepository;
import hello.upload.domain.UploadFile;
import hello.upload.file.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.UriUtil;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemController {
    private final ItemRepository itemRepository;
    private final FileStore fileStore;

    @GetMapping("/items/new")
    public String newItem(@ModelAttribute ItemForm form){
        return "item-form";
    }

   @PostMapping("/items/new")
    public String saveItem(@ModelAttribute ItemForm form, RedirectAttributes redirectAttributes) throws IOException {
        //파일 스토어에 파일 저장.파일자체는 파일스토리지에 저장을해.
       //form에서 받은 attach file, imageFile 은 MulitiPartFile 타입이야. 이걸 filestore에 저장하면서 타입을 UploadFile로 변환.
       UploadFile attachFile = fileStore.storeFile(form.getAttachFile());
       List<UploadFile> storeImageFiles = fileStore.storeFiles(form.getImageFiles());


       //실제 파일을 db에 저장하지 않아. 파일에 저장된 경로 같은거만 저장
       //db에 저장
       Item item = new Item();
       item.setItemName(form.getItemName());
       item.setAttachFile(attachFile);
       item.setImageFiles(storeImageFiles);
       itemRepository.save(item);

       redirectAttributes.addAttribute("itemId",item.getId());
       return "redirect:/items/{itemId}";
   }

   @GetMapping("/items/{id}")
    public String items(@PathVariable Long id, Model model){
       Item item = itemRepository.findById(id);
       model.addAttribute("item",item);
       return "item-view";
   }

    @ResponseBody
    @GetMapping("/images/{filename}")
    public Resource downloadImage(@PathVariable String filename) throws MalformedURLException {
        return new UrlResource("file:"+fileStore.getFullPath(filename));
    }

    @GetMapping("/attach/{itemId}")
    public ResponseEntity<Resource> downloadAttach(@PathVariable Long itemId) throws MalformedURLException {
        Item item = itemRepository.findById(itemId);
        String storeFileName = item.getAttachFile().getStoreFileName();
        String uploadFileName = item.getAttachFile().getUploadFileName();

        UrlResource resource = new UrlResource("file:" + fileStore.getFullPath(storeFileName));
        String encodedUploadFileName = UriUtils.encode(uploadFileName, StandardCharsets.UTF_8);
        String contentDisporition = "attachment; filename=\""+encodedUploadFileName+"\"";
        log.info("contentDisporition: {}",contentDisporition);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,contentDisporition)
                .body(resource);
    }


}
