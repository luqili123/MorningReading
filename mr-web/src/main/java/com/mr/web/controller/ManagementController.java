package com.mr.web.controller;

import com.mr.adapter.DateTimeAdapter;
import com.mr.common.Result;
import com.mr.common.ResultStatus;
import com.mr.mybatis.dto.ActivityResult;
import com.mr.mybatis.model.Material;
import com.mr.service.MaterialService;
import com.mr.service.SignInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

/**
 * @author lql
 * @date 2019/12/19 16:39
 */
@Controller
@RequestMapping("/manage")
@CrossOrigin
public class ManagementController {

    @Autowired
    private MaterialService materialService;
    @Autowired
    private SignInService signInService;
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    @RequestMapping(value = "/upload",method = RequestMethod.POST)
    @ResponseBody
    public Result<Boolean> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam("mid")Integer mid,
                                  @RequestParam("filename") String name,
                                  HttpServletRequest request) {

        //获取文件存储位置
        String path=request.getSession().getServletContext().getRealPath("materials");
        String originalFileName=file.getOriginalFilename();
        String filename=UUID.randomUUID().toString()+originalFileName;
        File newFile=new File(path,filename);
        if(!newFile.exists()){
            newFile.mkdirs();
        }
        Result<Boolean> result;

        try {
            //转存文件
            file.transferTo(newFile);
            //数据库操作
            Material material=new Material();
            material.setName(name);
            material.setMid(mid);
            material.setFilePath(path+File.separator+filename);
            if(materialService.uploadMaterial(material)){
                result=new Result<>(ResultStatus.SUCCESS,true);
            }else {
                result=new Result<>(ResultStatus.DATE_FORMAT_ERROR,false);
                //数据写入失败，删除刚刚上传的文件
                File f=new File(path);
                File[] lists=f.listFiles();
                for(File file1 :lists){
                    if(file1.getName().equals(filename)){
                        file1.delete();
                    }
                }
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            result=new Result<>(ResultStatus.UPLOAD_ERROR,false);
            newFile.delete();
            return result;
        }


    }

    /***
     * 创建活动，写入日期和创建者信息
     * @param date
     * @param uid
     * @return
     */
    @RequestMapping(value = "/createActivity",method = RequestMethod.POST)
    @ResponseBody
    public Result<Boolean> createActivity(@RequestParam("date") String date,
                                         @RequestParam(value = "uid",required = false) String uid){
        Result<Boolean> result;
        if(date.equals("")){
            result=new Result<>(ResultStatus.PARAMETER_ILLEGAL,false);
            return  result;
        }
        //转换日期格式
        DateTimeAdapter dateTimeAdapter=new DateTimeAdapter();
        try {
            date=dateTimeAdapter.dateFormat(date);
        } catch (ParseException e) {
            e.printStackTrace();
            result=new Result<>(ResultStatus.DATE_FORMAT_ERROR,false);
        }

        Material material=new Material();
        material.setUploadTime(date);
        material.setUid(uid);

        if(materialService.createActivity(material)){
            result =new Result<>(ResultStatus.SUCCESS,true);
        }else{
            result=new Result<>(ResultStatus.DATABASE_WRITE_ERROR,false);
        }
        return result;
    }

    /***
     * 返回某天的晨读材料名称和签到人员名单
     * @param mid
     * @return
     */
    @RequestMapping(value = "/activityResult/{mid}",method = RequestMethod.GET)
    @ResponseBody
    public Result<ActivityResult> activityResult(@PathVariable("mid") Integer mid){
        Result<ActivityResult> result;
        ActivityResult activityResult=new ActivityResult();
        //查询上传的文件名
        Material material=materialService.findByMid(mid);
        //查询当天签到的人员名单
        List<String> signList=signInService.findUidByDate(material.getUploadTime());
        //将结果存入对象
        activityResult.setFileName(material.getName());
        activityResult.setSignList(signList);
        //封装返回对象
        result=new Result<>(ResultStatus.SUCCESS,activityResult);
        return result ;

    }

    /***
     * 获取晨读材料列表（管理员日期列表）
     * @return
     */
    @RequestMapping(value = "/displayActivities",method = RequestMethod.GET)
    @ResponseBody
    public Result<List<Material>> displayActivities(){
        List<Material> materialList=materialService.findAll();
        Result<List<Material>> result;
        if(materialList.size()==0){
            result=new Result<>(ResultStatus.ARTICLE_NOT_EXIST,materialList);
        }
        result=new Result<>(ResultStatus.SUCCESS,materialList);
        return result;
    }

}
