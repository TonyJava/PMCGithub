package scut.zengxi.Funnel;

import com.pmc.Activityroute.service.ActivityRouteInfoBasicService;
import org.junit.Test;
import scut.zengxi.FPSTree.StringUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
/**
 * 
 * @author zengxi
 *
 *	漏斗模型的实现，目前没有实现七日数据保存
 *	友盟定义漏斗的有效期是7天，即用户触发初始事件后有7天的时间来完成漏斗，7天后完成的转化不会被计算在该漏斗内。
 *
 */
public class Funnel {
	private String  criticalPath;     //关键路径
	private static Map<String,Integer>countMap = new HashMap<String, Integer>();    //关键路径上的关键节点的频率计算和关键转换的频率计算

	public Funnel(){}
	/*public Funnel(String criticalPath) {
		this.criticalPath=criticalPath;
	}*/
	/**
	 * 获取关键路径
	 * @return
	 */
	public String getCriticalPath() {
		String []nodes=this.criticalPath.split(",");
		if(nodes.length<2)
			System.out.println("关键路径以逗号分隔并且不能少于两个关键节点！");
		return criticalPath;
	}
	/**
	 * 设置关键路径
	 * @param criticalPath
	 */
	public void setCriticalPath(String criticalPath) {
		this.criticalPath = criticalPath;
	}
	/**
	 * 获取关键路径上的关键节点
	 * @return   关键节点
	 */
	public String[] getNodes(){
		String []nodes=this.criticalPath.split(",");
		return nodes;
	}
	/**
	 * 初始化计算频数的map
	 * @param nodes  关键节点
	 */
	public void initCountMap(String []nodes){
		//初始化单点的频数
		for (int i = 0; i < nodes.length; i++) {
			countMap.put(nodes[i], 0);
		}
		//初始化两点间的转化频数
		for (int i = 0; i < nodes.length-1; i++) {
			countMap.put(nodes[i]+"-"+nodes[i+1], 0);
		}
		
	}
	
	/**
	 * 计算关键转化的频数
	 * @param pre  关键转化的前节点位置
	 * @param next  关键转化的后节点位置
	 * @return   关键转化的频数
	 */
	public int getPairs(int []pre,int []next){
		int count=0;
		for (int i = 0,j=0; i < pre.length&&j<next.length; j++) {
			if(pre[i]<next[j]){
				count++;
				i++;
			}
		}
		return count;
	}
	
	/**
	 * 计算数据集中关键节点，关键转化的总频数
	 * @param records    数据集的记录
	 * @param nodes      关键节点
	 */
	public void getStatics(List<List<String>> records,String []nodes){
		initCountMap(nodes);
		for (int i = 0; i < records.size(); i++) {
			//将list类型的记录转换为string类型
			String pattern = StringUtil.list2Str(records.get(i));
			//System.out.println("current pattern:"+pattern);
			for (int j = 0; j < nodes.length; j++) {
				int org=0;
				if(indexsOf(pattern,nodes[j])!=null)
				{
					//计算pattern中出现nodes[j]的次数
					org=indexsOf(pattern,nodes[j]).length;
				}
				//循环加上每条记录中出现的关键节点的频数
				countMap.put(nodes[j], countMap.get(nodes[j])+org);
			}
			
			for (int j = 0; j < nodes.length-1; j++) {
				int org=0;
				if(indexsOf(pattern,nodes[j])!=null&&indexsOf(pattern,nodes[j+1])!=null)
				{
					int [] pre=indexsOf(pattern,nodes[j]);
					int [] next=indexsOf(pattern,nodes[j+1]);
					//计算pattern中出现nodes[i]-nodes[j]的次数
					org = getPairs(pre, next);
					
				}
				//循环加上每条记录中出现的关键节点间转化的频数
				countMap.put(nodes[j]+"-"+nodes[j+1], countMap.get(nodes[j]+"-"+nodes[j+1])+org);
			}
		}
		//输出计数器，测试验证用
		//System.out.println(countMap);
	}
	/**
	 * 计算各关键转化的转化率
	 * @param nodes  关键节点
	 * @return  返回float[]型转化率数组
	 */
	public Float[] getConvers(String []nodes){
		
		if(nodes.length>0){
			Float []convers= new Float[nodes.length];
			convers[0]=100.0f;
			for (int i = 0; i < nodes.length-1; i++) {
				convers[i+1]=((float)(countMap.get(nodes[i]+"-"+nodes[i+1])))/((float)countMap.get(nodes[i]))*100.0f;
				//修复无穷大的异常
				if(convers[i+1].isNaN())convers[i+1]=0.0f;
			}
			/*for (int i = 0; i < convers.length; i++) {
				System.out.print(convers[i]+" ");
			}*/
			return convers;
		}else{
			return null;
		}
	}

	/**
	 * 获取关键路径转化率的函数
	 * @param convers  相邻关键节点间的转化率
	 * @return   总关键路径的转化率
	 */
	public float getConverRate(Float[]convers){
		if(convers==null||convers.length==0)
			return 0.0f;
		else{
			float res=1.0f;
			for (int i = 0; i < convers.length; i++) {
				res*=(convers[i]/100);
			}
			//以百分数的形式返回
			res*=100.f;
			return res;
		}
	}

	/**
	 * 以数组的形式返回某字符串中出现某子串的全部位置
	 * @param pattern  主串
	 * @param str  子串
	 * @return  位置数组
	 */
	public int[] indexsOf(String pattern,String str){
		String string="";
		for(int i=0;i<pattern.length();i++){
			i = pattern.indexOf(str,i);
			if(i<0)break;
			string+=i+",";
		}
		if(string.equals(""))return null;
		else{
			string=string.substring(0,string.length()-1);
			String[] resString=string.split(",");
			int [] res=new int[resString.length];
			for(int i=0;i<res.length;i++){
				res[i]=Integer.parseInt(resString[i]);
			}
			return res;
		}
	}


	/**
	 * 获取漏斗模型分析的所有结果的接口函数
	 * @return List<Map<String,List>>
	 */

	public List<Map<String,List>> getFunnelResult(){
		Funnel fun=new Funnel();
		//设置关键路径
		fun.setCriticalPath("AppStart,Main,NewsDetail,null");
		//获取数据集
		ActivityRouteInfoBasicService activityRouteInfoBasicService = new ActivityRouteInfoBasicService();
		List<List<String>> transRecords = activityRouteInfoBasicService.getRoutes("85d4a553-ee8d-4136-80ab-2469adcae44d");
		//获取关键路径的节点
		fun.getStatics(transRecords,fun.getNodes());
		String []criticalNodes=fun.getNodes();
		//获取单节点的统计结果
		List<Map<String,List>> results= new LinkedList<>();
		List<Map<String,Integer>> nodeCounts = new LinkedList<>();
		for (int i=0;i<criticalNodes.length;i++){
			Map<String,Integer> map = new HashMap<>();
			map.put(criticalNodes[i],countMap.get(criticalNodes[i]));
			nodeCounts.add(map);
		}
		Map<String,List> nodeCountsMap= new HashMap<>();
		nodeCountsMap.put("nodeCounts",nodeCounts);
		results.add(nodeCountsMap);

		//获取相邻节点的转化频数
		List<Map<String,Integer>> nodePaths = new LinkedList<>();
		for (int i=0;i<criticalNodes.length-1;i++){
			Map<String,Integer> map = new HashMap<>();
			map.put(criticalNodes[i]+"-"+criticalNodes[i+1],countMap.get(criticalNodes[i]+"-"+criticalNodes[i+1]));
			nodePaths.add(map);
		}
		Map<String,List> nodePathsMap= new HashMap<>();
		nodePathsMap.put("nodePaths",nodePaths);
		results.add(nodePathsMap);
		//获取相邻节点的转化率
		List<Map<String,String>> nodePathsRate = new LinkedList<>();
		Float convers[]=fun.getConvers(criticalNodes);
		for (int i=1;i<convers.length;i++){
			Map<String,String> map = new HashMap<>();
			String temp=convers[i].toString();
			if(temp.length()>=5)temp=temp.substring(0,5);
			map.put(criticalNodes[i-1]+"-"+criticalNodes[i],temp+"%");
			nodePathsRate.add(map);
		}
		Map<String,List> nodePathsRateMap= new HashMap<>();
		nodePathsRateMap.put("nodePathsRate",nodePathsRate);
		results.add(nodePathsRateMap);

		//获取整个关键路径的转化率
		List<Map<String,String>> pathRate = new LinkedList<>();
		Map<String,String> converRate = new HashMap<>();
		String tempRate=fun.getConverRate(convers)+"";
		if(tempRate.length()>=5)tempRate=tempRate.substring(0,5);
		converRate.put("pathConverRate",tempRate+"%");
		pathRate.add(converRate);
		Map<String,List> pathRateMap= new HashMap<>();
		pathRateMap.put("conversionRate",pathRate);
		results.add(pathRateMap);

		//System.out.println("=========================="+results);
		return results;
	}

	/**
	 * JUnit单元测试获取结果的函数
	 */
	@Test
	public void testGetResults(){
		System.out.println("获取到的关键路径分析结果如下：");
		System.out.println(getFunnelResult());
	}

}
