package com.pactera.core.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pactera.api.ws.approval.Action;
import com.pactera.api.ws.approval.ApprovalRecord;
import com.pactera.api.ws.approval.Category;
import com.pactera.api.ws.approval.Form;
import com.pactera.api.ws.approval.FormSummary;
import com.pactera.api.ws.approval.FormType;
import com.pactera.api.ws.approval.FormValue;
import com.pactera.api.ws.approval.ValueGroup;
import com.pactera.api.ws.apsAPI.ApplicationBaseEntityDto;
import com.pactera.api.ws.apsAPI.ApplicationTraceNodeEntityDto;
import com.pactera.api.ws.apsAPI.ProcessViewModel;
import com.pactera.core.entity.AppSSO;
import com.pactera.core.entity.K2ProcSet;
import com.pactera.core.entity.MgtUser;
import com.pactera.core.entity.MobileInterfaceConfig;
import com.pactera.core.entity.ProcessGroup;
import com.pactera.core.entity.params.DoActionParams;
import com.pactera.core.entity.params.GetFormCategoryParams;
import com.pactera.core.entity.params.GetFormDetailParams;
import com.pactera.core.entity.params.GetFormListParams;
import com.pactera.core.entity.vo.MyDoneBaseVo;
import com.pactera.core.entity.vo.MyDoneListVo;
import com.pactera.core.entity.vo.MyToDoBaseVo;
import com.pactera.core.entity.vo.MyToDoListVo;
import com.pactera.core.enums.ApplicationStatusEnum;
import com.pactera.core.enums.ApprovalActionEnum;
import com.pactera.core.mapper.IAppSSOMapper;
import com.pactera.core.mapper.IApprovalMapper;
import com.pactera.core.mapper.IK2ProcSetMapper;
import com.pactera.core.mapper.IMgtUserMapper;
import com.pactera.core.mapper.IMobileInterfaceConfigMapper;
import com.pactera.core.mapper.IProcessGroupMapper;
import com.pactera.core.service.IApprovalService;
import com.pactera.core.service.ICxfClientService;
import com.pactera.core.service.IEERPInterfaceService;
import com.pactera.core.utils.HttpRequest;
import com.pactera.core.utils.MyDateUtil;
import com.pactera.core.utils.MyException;

/**
 * 审批
 * @author qys
 * @date 2016年6月17日 上午10:58:48
 */
@Service
public class ApprovalServiceImpl implements IApprovalService{
	
	private static final Logger log = Logger.getLogger(ApprovalServiceImpl.class);
	
	/**
	 * 发送类别
	 * @author qys
	 * @date 2016年6月17日 上午10:53:24
	 * @param params 传入的参数
	 */
	public void sendCategory(GetFormCategoryParams params) throws Exception{
		// 根据用户AD查询用户的ID
		MgtUser mgtUser = this.mgtUserMapper.getMgtUserByAd(params.getUserAd());
//		IApprovalAPI approvalAPI = ProxyUtil.getIApprovalAPI(params.getCallbackAddress());
		
		List<ProcessGroup> processGroups = this.processGroupMapper.getAllProcessGroups();
		if (null == processGroups) {
			throw new MyException("获取流程分作失败!");
		}
		List<Category> categories = new ArrayList<Category>();
		List<?> list = this.approvalMapper.getMyToDoListVos(mgtUser.getAd(), "", "(1=1)", 1, 14, 0, 1, 0, "createdON desc");
		MyToDoBaseVo daiBan = null;
		if (null != list && list.size() > 0) {
			daiBan = new MyToDoBaseVo(list);
		}
		int daiBanCount = 0;
		if (null != daiBan) {
			daiBanCount = daiBan.getDataCount();
		}
		// 目前只实现权限自动化，因此写入固定的即可
		Category c = new Category();
		// 组织数据
		c.setId(14 + ""); // 分类标识
		c.setName("权限流程"); // 分类名称
		c.setUnprocessedCount(daiBanCount); // 未处理的数量
		c.setHasTrackingList(false);
		c.setDraftCount(0);
		categories.add(c);
		
		// --------------------------------------------------------------------------------------------------------------------------
		// 以下是按照组的概念，返回分类信息
		/*for (ProcessGroup pg : processGroups) {
			System.out.println("***********************************************************");
			// 此处循环查询待办、已办数量，如果待办+已办均为0则不进行前台显示
			// 待办
			List<?> list = this.approvalMapper.getMyToDoListVos(mgtUser.getAd(), "", "(1=1)", 1, pg.getProcessGroupID(), 0, 1, 0, "createdON desc");
			MyToDoBaseVo daiBan = null;
			if (null != list && list.size() > 0) {
				daiBan = new MyToDoBaseVo(list);
			}
			// 已办
			list = this.approvalMapper.getMyDoneListVos(mgtUser.getId(),  "", "(1=1)", -1, pg.getProcessGroupID(), 0, 1, 0, "OperationTime DESC");
			MyDoneBaseVo yiBan = null;
			if (null != list && list.size() > 0) {
				yiBan = new MyDoneBaseVo(list);
			}
			if (null == yiBan && null == daiBan) {
				continue;
			}
			int daiBanCount = daiBan.getDataCount();
			int yiBanCount = yiBan.getDataCount();
			if (0 == daiBanCount && 0 == yiBanCount) {
				continue;
			}
			if (pg.getProcessGroupID() != 14) {
				continue;
			}
			Category c = new Category();
			// 组织数据
			c.setId(pg.getProcessGroupID() + ""); // 分类标识
			c.setName(pg.getProcessGroupName()); // 分类名称
			c.setUnprocessedCount(daiBanCount); // 未处理的数量
			c.setHasTrackingList(false);
			c.setDraftCount(0);
			categories.add(c);
		}*/
		// 发送数据
		this.cxfClientService.sendFormCategory(params, categories);
	}
	
	/**
	 * 获取数据列表
	 * @author qys
	 * @date 2016年6月17日 上午10:58:20
	 * @param sessionId
	 * @param callUrl 回调地址
	 * @param parameters 参数【接口传入的参数，每个调用地址的参数可能不同】
	 */
	public void sendFormList(GetFormListParams params) throws Exception{
		// 根据用户AD查询用户的ID
		MgtUser mgtUser = this.mgtUserMapper.getMgtUserByAd(params.getUserAd());
		if (null == mgtUser) {
			throw new MyException("获取用户信息失败!");
		}
		// 类别ID
		int processGroupId = Integer.parseInt(params.getCategoryId());
		List<FormSummary> formList = new ArrayList<FormSummary>();
		List<?> list = null;
		FormType ft = null;
		switch (params.getProcessStatus()) {
			case draft: // 草稿
				System.out.println("草稿");
				ft = FormType.DRAFT;
				break;
			case need_to_approve: // 待办
				System.out.println("待办");
				ft = FormType.NEED_TO_APPROVE;
				list = this.approvalMapper.getMyToDoListVos(params.getUserAd(), "", "(1=1)", 1, processGroupId, 0, params.getCount(), params.getBeginIndex(), "createdON desc");
				MyToDoBaseVo mtdbv = null;
				// 取出数据
				if (null != list && list.size() > 0) {
					mtdbv = new MyToDoBaseVo(list);
				}
				// 组织数据
				if (null != mtdbv && mtdbv.getDataCount() != 0 && null != mtdbv.getMyToDoListVos() && mtdbv.getMyToDoListVos().size() > 0) {
					for (MyToDoListVo mtl : mtdbv.getMyToDoListVos()) {
						FormSummary fs = new FormSummary();
						fs.setType(ft);
						fs.setId(mtl.getApplicationNumber());
						fs.setCategoryId(params.getCategoryId());
						fs.setSubject(mtl.getApplicationSubject());
						fs.setSubmitter(mtl.getApplicantName());
						fs.setSubmitTime(mtl.getCreatedOn());
						fs.setLastApprover(mtl.getCurrentActivity());
						fs.setLastApprovalTime(null);
						fs.setReadonly(false);
						formList.add(fs);
					}
				}
				break;
			case approved: // 已办
				System.out.println("已办");
				ft = FormType.APPROVED;
				list = this.approvalMapper.getMyDoneListVos(mgtUser.getId(),  "", "(1=1)", -1, processGroupId, 0, params.getCount(), params.getBeginIndex(), "OperationTime DESC");
				MyDoneBaseVo mdbv = null;
				// 取出数据
				if (null != list && list.size() > 0) {
					mdbv = new MyDoneBaseVo(list);
				}
				// 组织数据
				if (null != mdbv && mdbv.getDataCount() != 0 && null != mdbv.getDoneListVos() && mdbv.getDoneListVos().size() > 0) {
					for (MyDoneListVo mtl : mdbv.getDoneListVos()) {
						FormSummary fs = new FormSummary();
						fs.setType(ft);
						fs.setId(mtl.getApplicationNumber());
						fs.setCategoryId(params.getCategoryId());
						fs.setSubject(mtl.getApplicationSubject());
						fs.setSubmitter(mtl.getApplicantName());
						fs.setSubmitTime(mtl.getCreatedOn());
						fs.setLastApprovalTime(null);
						fs.setReadonly(false);
						formList.add(fs);
					}
				}
				break;
			case tracking:
				ft = FormType.TRACKING;
				break;
			default:
				throw new MyException("参数传入错误!");
		}
		this.cxfClientService.sendFormList(params, formList);
	}
	
	/**
	 * 发送表单详情
	 * @author qys
	 * @date 2016年6月17日 上午10:58:21
	 * @param sessionId
	 */
	public void sendFormDetail(GetFormDetailParams params) throws Exception{
		String applicationNumber = params.getFormId();
		if (StringUtils.isBlank(applicationNumber)) {
			return;
		}
		// 根据登录的用户帐号查询Token
		AppSSO sso = this.appSSOMapper.getAppSSOByLoginUserAD(params.getUserAd());
		if (null == sso) {
			log.error(params.getUserAd() + "获取token失败!");
			throw new MyException("用户登录失败!请联系管理员!");
		}
		if (StringUtils.isBlank(sso.getAuthvalue())) {
			log.error(params.getUserAd() + "获取token失败!");
			throw new MyException("用户登录失败!请联系管理员!");
		}
		// 根据流水号查询数据
		K2ProcSet k2 = this.k2ProcSetMapper.getK2ProcSetByApplicationNumber(applicationNumber);
		Form form = new Form();
		
		
		ProcessViewModel pvm = null;
		// 审批历史记录
		pvm = this.eerpIeerpInterfaceService.getProcessViewModelBySerial_number(applicationNumber);
		if (null == pvm) {
			throw new MyException("获取审批记录失败!");
		}
		// ------ 继承的基本信息
		ApplicationBaseEntityDto abed = pvm.getBaseEntity(); // 流程信息
		//-----------------abed.getProcessID();
		
		form.setId(abed.getApplicationNumber()); // 流水号
		form.setCategoryId(params.getCategoryId()); // 类别ID
		form.setSubject(abed.getApplicationSubject()); // 主题
		form.setSubmitter(abed.getApplicantName()); // 申请人
		form.setSubmitTime(abed.getCreatedOn().toString()); // 提交时间
		if (ApplicationStatusEnum.Running == ApplicationStatusEnum.getStatusByKey(abed.getApplicationStatus())) {
			// 根据流水号，查询是否支持移动审批
			
			
			form.setType(FormType.NEED_TO_APPROVE);
			Action action = new Action();
			action.setName(ApprovalActionEnum.agree.getDesc());// 审批
			action.setKey(ApprovalActionEnum.agree.toString());
			form.getActions().add(action);
			action = new Action();
			action.setName(ApprovalActionEnum.Refuse.getDesc());// 拒绝
			action.setKey(ApprovalActionEnum.Refuse.toString());
			form.getActions().add(action);
			form.setReadonly(false); // 只读状态
		}else{
			form.setType(FormType.APPROVED);
			form.setReadonly(true); // 只读状态
		}
		// ------ 申请信息
		ValueGroup vg = new ValueGroup();
		vg.setName("申请基本信息");
		vg.setExpand(true);
		FormValue fv = new FormValue();
		fv.setName("状态");
		fv.setValue(ApplicationStatusEnum.getStatusByKey(abed.getApplicationStatus()).getDesc());
		vg.getValues().add(fv);
		
		fv = new FormValue();
		fv.setName("日期");
		fv.setValue(abed.getCreatedOn().toString());
		vg.getValues().add(fv);
		
		fv = new FormValue();
		fv.setName("姓名");
		fv.setValue(abed.getApplicantAD());
		vg.getValues().add(fv);
		
		fv = new FormValue();
		fv.setName("电话");
		fv.setValue(abed.getApplicantPhone());
		vg.getValues().add(fv);
		
		fv = new FormValue();
		fv.setName("邮箱");
		fv.setValue(abed.getApplicantEmail());
		vg.getValues().add(fv);
		
		// 加入到Form对象中
		form.getValueGroups().add(vg);
		// ------ 审批记录
		List<ApplicationTraceNodeEntityDto> applicationLogEntityDto = pvm.getApplicationLogList().getApplicationTraceNodeEntityDto(); // 审批日志
		for (ApplicationTraceNodeEntityDto ate : applicationLogEntityDto) {
			ApprovalRecord ar = new ApprovalRecord();
			ar.setUsername(ate.getDestinationUserDesc() + ate.getNodeName()); // 操作人
			ar.setAction(StringUtils.isBlank(ate.getActionName()) ? "待审批" : ate.getActionName());// 审批动作 actionName
			ar.setOpinion(ate.getComments()); // 审批意见
			String operateTime = MyDateUtil.dbTimerToFmtShowMin(MyDateUtil.dateFmtTimer(new Date()));
			ar.setOperateTime(StringUtils.isBlank(ate.getActionTime()) ? operateTime : ate.getActionTime()); // 审批时间
			form.getHistory().add(ar);
		}
		
		// 根据Folder获取调用地址
		MobileInterfaceConfig mic = this.mobileInterfaceConfigMapper.getMobileInterfaceConfigByFolderName(k2.getFolder());
		if (null == mic) {
			log.error("获取详情失败!地址接口获取为空!");
			throw new MyException("获取详情失败!请联系管理员!");
		}
		if (StringUtils.isBlank(mic.getUrl())) {
			log.error("获取接口地址失败!地址没有配置!");
			throw new MyException("获取详情失败!请联系管理员!");
		}
		// 设置参数
		StringBuffer param = new StringBuffer();
		param.append("Token=");
		param.append(sso.getAuthvalue());
		param.append("&applicationNumber=");
		param.append(abed.getApplicationNumber());
		param.append("&procSetName=");
		param.append(k2.getName());
		param.append("&processId=");
		param.append(abed.getProcessID());
		// 获取json对象
		String jsonStr = HttpRequest.sendGet(mic.getUrl(), param.toString());
		if (null == jsonStr) {
			throw new MyException("获取详情数据失败！");
		}
		List<ValueGroup> groups = null;
		String o = JSONObject.parse(jsonStr).toString();
		groups = new Gson().fromJson(o, new TypeToken<List<ValueGroup>>(){}.getType());
		if (null != groups && groups.size() > 0) {
			for (ValueGroup vv : groups) {
				form.getValueGroups().add(vv);
			}
		}
		// 根据流水号查询附件
		
		
		// 根据Name 判断应该查询哪张表
//		if ("AORWF".equals(k2.getFolder())) { // 权限自动化
//			if ("PCA".equals(k2.getName())) { // 权限变更
//				form = this.changeThePermissionsService.getFormByApplicationNumber(applicationNumber, form);
//			}else if ("ARA".equals(k2.getName())) {// 权限申请
//				form = this.accountApplicationService.getFormByApplicationNumber(applicationNumber, form);
//			}else{
//				throw new MyException("数据传入错误!");
//			}
//		}
//		System.out.println(new Gson().toJson(form));
		this.cxfClientService.sendFormDetail(params, form);
	}
	
	/**
	 * 审批动作
	 * @author qys
	 * @date 2016年7月12日 下午3:58:44
	 * @param params
	 */
	public void doAction(DoActionParams params) throws Exception{
		// 根据流水号查询数据
		//this.eerpIeerpInterfaceService.getProcessViewModelBySerial_number(params.getFormId());
		
		String sn = this.approvalMapper.getSNByUserAdAndApplicationNumber(params.getUserAd(), params.getFormId());
		if (StringUtils.isBlank(sn)) {
			throw new MyException("此数据已审批!");
		}
		switch (params.getAction()) {
		case agree:
			System.out.println("成功调用审批接口");
			this.eerpIeerpInterfaceService.approve(params.getUserAd(), params.getOpinion(), params.getFormId(), sn, true);
			break;
		case Refuse:
			System.out.println("成功调用拒绝接口");
			this.eerpIeerpInterfaceService.approve(params.getUserAd(), params.getOpinion(), params.getFormId(), sn, false);
			break;
		default:
			throw new MyException("非法操作!");
		}
		
		
	}

	/**
	 * 用户
	 */
	@Autowired
	private IMgtUserMapper mgtUserMapper;
	
	/**
	 * 流程分组
	 */
	@Autowired
	private IProcessGroupMapper processGroupMapper;
	
	/**
	 * 审批数据查询
	 */
	@Autowired
	private IApprovalMapper approvalMapper;
	
	/**
	 * K2中的系统所在库表
	 */
	@Autowired
	private IK2ProcSetMapper k2ProcSetMapper;
	
	/**
	 * eerp平台接口
	 */
	@Autowired
	private IEERPInterfaceService eerpIeerpInterfaceService;
	
	/**
	 * 用户Token登录
	 */
	@Autowired
	private IAppSSOMapper appSSOMapper;
	
	/**
	 * 根据K2文件夹获取接口地址
	 */
	@Autowired
	private IMobileInterfaceConfigMapper mobileInterfaceConfigMapper;
	
	/**
	 * 客户端封装
	 */
	@Autowired
	private ICxfClientService cxfClientService;
	
}
