/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.JobExecutorContext;
import org.activiti.engine.impl.persistence.deploy.ProcessDefinitionInfoCacheObject;
import org.activiti.engine.impl.pvm.runtime.InterpretableExecution;

import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public class Context {

  protected static ThreadLocal<Stack<CommandContext>> commandContextThreadLocal = new ThreadLocal<Stack<CommandContext>>();
  protected static ThreadLocal<Stack<ProcessEngineConfigurationImpl>> processEngineConfigurationStackThreadLocal = new ThreadLocal<Stack<ProcessEngineConfigurationImpl>>();
  protected static ThreadLocal<Stack<ExecutionContext>> executionContextStackThreadLocal = new ThreadLocal<Stack<ExecutionContext>>();
  protected static ThreadLocal<JobExecutorContext> jobExecutorContextThreadLocal = new ThreadLocal<JobExecutorContext>();
  protected static ThreadLocal<Map<String, ObjectNode>> bpmnOverrideContextThreadLocal = new ThreadLocal<Map<String, ObjectNode>>();

  public static CommandContext getCommandContext() {
    Stack<CommandContext> stack = getStack(commandContextThreadLocal);
    if (stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  public static void setCommandContext(CommandContext commandContext) {
    getStack(commandContextThreadLocal).push(commandContext);
  }

  public static void removeCommandContext() {
    getStack(commandContextThreadLocal).pop();
  }

  public static ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    Stack<ProcessEngineConfigurationImpl> stack = getStack(processEngineConfigurationStackThreadLocal);
    if (stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  public static void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
    getStack(processEngineConfigurationStackThreadLocal).push(processEngineConfiguration);
  }

  public static void removeProcessEngineConfiguration() {
    getStack(processEngineConfigurationStackThreadLocal).pop();
  }

  public static ExecutionContext getExecutionContext() {
    return getStack(executionContextStackThreadLocal).peek();
  }
  
  public static boolean isExecutionContextActive() {
  	Stack<ExecutionContext> stack = executionContextStackThreadLocal.get();
  	return stack != null && !stack.isEmpty();
  }

  public static void setExecutionContext(InterpretableExecution execution) {
    getStack(executionContextStackThreadLocal).push(new ExecutionContext(execution));
  }

  public static void removeExecutionContext() {
    getStack(executionContextStackThreadLocal).pop();
  }

  protected static <T> Stack<T> getStack(ThreadLocal<Stack<T>> threadLocal) {
    Stack<T> stack = threadLocal.get();
    if (stack==null) {
      stack = new Stack<T>();
      threadLocal.set(stack);
    }
    return stack;
  }
  
  public static JobExecutorContext getJobExecutorContext() {
    return jobExecutorContextThreadLocal.get();
  }
  
  public static void setJobExecutorContext(JobExecutorContext jobExecutorContext) {
    jobExecutorContextThreadLocal.set(jobExecutorContext);
  }
  
  public static void removeJobExecutorContext() {
    jobExecutorContextThreadLocal.remove();
  }
  
  public static ObjectNode getBpmnOverrideElementProperties(String id, String processDefinitionId) {
    Map<String, ObjectNode> bpmnOverrideMap = getBpmnOverrideContext();
    if (bpmnOverrideMap.containsKey(processDefinitionId) == false) {
      ProcessDefinitionInfoCacheObject cacheObject = getProcessEngineConfiguration().getDeploymentManager()
          .getProcessDefinitionInfoCache()
          .get(processDefinitionId);
      
      addBpmnOverrideElement(processDefinitionId, cacheObject.getInfoNode());
    }
    
    ObjectNode definitionInfoNode = getBpmnOverrideContext().get(processDefinitionId);
    ObjectNode elementProperties = null;
    if (definitionInfoNode != null) {
      elementProperties = getProcessEngineConfiguration().getDynamicBpmnService().getElementProperties(id, definitionInfoNode);
    }
    return elementProperties;
  }
  
  public static Map<String, ObjectNode> getBpmnOverrideContext() {
    Map<String, ObjectNode> bpmnOverrideMap = bpmnOverrideContextThreadLocal.get();
    if (bpmnOverrideMap == null) {
      bpmnOverrideMap = new HashMap<String, ObjectNode>();
    }
    return bpmnOverrideMap;
  }
  
  public static void addBpmnOverrideElement(String id, ObjectNode infoNode) {
    Map<String, ObjectNode> bpmnOverrideMap = bpmnOverrideContextThreadLocal.get();
    if (bpmnOverrideMap == null) {
      bpmnOverrideMap = new HashMap<String, ObjectNode>();
      bpmnOverrideContextThreadLocal.set(bpmnOverrideMap);
    }
    bpmnOverrideMap.put(id, infoNode);
  }
  
  public static void removeBpmnOverrideContext() {
    bpmnOverrideContextThreadLocal.remove();
  }
}
