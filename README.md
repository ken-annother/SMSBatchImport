# InsertTool 说明

---


本工具用来批量导入短信。

This tool used to bulk import SMS.



支持导入的短信文件格式有两种：

There are two types of SMS file formats that support import:


* .txt或者.csv文件后缀。 用tab分隔短信号码和短信内容

.txt or.csv file suffix.Use tabs to separate text messages from Numbers

		95566	【中国银行】您个人账户0957于2016-04-15日16:15分转账至个人账户7579人民币260000元整请注意查收。
		IM-ICICIB	Dear Customer, your Account XX4657 has been debited with INR 2,002.95 on 17-Mar-18. Info: BIL*001413926733*NchgRs2.50G. The Available Balance is INR 3,39,774.12.

* .xml文件后缀。 类似于html标签

.xml file suffix.Similar to HTML tags
		
		<string name="95566"><![CDATA[【中国银行】您个人账户0957于2016-04-15日16:15分转账至个人账户7579人民币260000元整请注意查收。]]></string>
		<string name="IM-ICICIB"><![CDATA[Dear Customer, your Account XX4657 has been debited with INR 2,002.95 on 17-Mar-18. Info: BIL*001413926733*NchgRs2.50G. The Available Balance is INR 3,39,774.12.]]></string>


写入文件时，一条短信一行。

When writing to a file, you should write in line on each message.
