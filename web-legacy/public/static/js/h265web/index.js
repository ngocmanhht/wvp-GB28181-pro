/********************************************************* 
 * LICENSE: LICENSE-Free_CN.MD
 * 
 * Author: Numberwolf - ChangYanlong
 * QQ: 531365872
 * QQ Group:925466059
 * Wechat: numberwolf11
 * Discord: numberwolf#8694
 * E-Mail: porschegt23@foxmail.com
 * Github: https://github.com/numberwolf/h265web.js
 * 
 * Author: little tiger(Numberwolf)(Chang Yanlong)
 * QQ: 531365872
 * QQgroup: 531365872
 * WeChat: numberwolf11
 * Discord: numberwolf#8694
 * Email: porschegt23@foxmail.com
 * Blog: https://www.jianshu.com/u/9c09c1e00fd1
 * Github: https://github.com/numberwolf/h265web.js
 * 
 **********************************************************/
require('./h265webjs-v20221106');
export default class h265webjs {
	static createPlayer(videoURL, config) {
		return window.new265webjs(videoURL, config);
	}

	static clear() {
		global.STATICE_MEM_playerCount = -1;
		global.STATICE_MEM_playerIndexPtr = 0;
    }
}
