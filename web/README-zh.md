# vue-admin-template

> This is a minimalist vue admin management backend. It only contains Element UI & axios & iconfont & permission control & lint, which are necessary to build the backend.

 [online address](http://panjiachen.github.io/vue-admin-template) 

 [Domestic visit](https://panjiachen.gitee.io/vue-admin-template) 

The current version is `v4.0+` , which is built based on `vue-cli` . If you want to use an older version, you can switch the branch to [tag/3.11.0](https://github.com/PanJiaChen/vue-admin-template/tree/tag/3.11.0) , which does not depend on `vue-cli` .

<p align="center">
  <b>SPONSORED BY</b>
</p>
<p align="center">
   <a href="https://finclip.com?from=vue_element" title="FinClip" target="_blank">
      <img height="200px" src="https://gitee.com/panjiachen/gitee-cdn/raw/master/vue%E8%B5%9E%E5%8A%A9.png" title="FinClip">
   </a>
</p>

## Extra

If you want to dynamically generate sidebars and routers based on user roles, you can use this branch [permission-control](https://github.com/PanJiaChen/vue-admin-template/tree/permission-control) 

## Related projects

- [vue-element-admin](https://github.com/PanJiaChen/vue-element-admin)

- [electron-vue-admin](https://github.com/PanJiaChen/electron-vue-admin)

- [vue-typescript-admin-template](https://github.com/Armour/vue-typescript-admin-template)

- [awesome-project](https://github.com/PanJiaChen/vue-element-admin/issues/2312)

Wrote a series of tutorial supporting articles on how to build a complete backend project from scratch:

- [Hand in hand, I will show you how to use Vue to master the backend. Series 1 (Basics)](https://juejin.im/post/59097cd7a22b9d0065fb61d2) 
- [Hand in hand, I will show you how to use vue to control the backend. Series 2 (Login Permissions)](https://juejin.im/post/591aa14f570c35006961acac) 
- [Hand in hand, I will show you how to use Vue to master the backend. Series 3 (Practice)](https://juejin.im/post/593121aa0ce4630057f70d35) 
- [Hands-on, I will show you how to use vue to build the backend. Series 4 (vueAdmin is a minimalist backend basic template. Articles specifically for this project are counted as one document)](https://juejin.im/post/595b4d776fb9a06bbe7dba56) 
- [Hands-on, I will help you encapsulate a vue component](https://segmentfault.com/a/1190000009090836) 

## Build Setup

```bash
# Clone project
git clone https://github.com/PanJiaChen/vue-admin-template.git

# Enter the project directory
cd vue-admin-template

# Install dependencies
npm install

# It is recommended not to use cnpm directly since there will be various weird bugs after installation. You can solve the problem of slow npm download speed by doing the following
npm install --registry=https://registry.npm.taobao.org

# Start service
npm run dev
```

Browser access [http://localhost:9528](http://localhost:9528) 

## Publish

```bash
# Build test environment
npm run build:stage

# Build a production environment
npm run build:prod
```

## Others

```bash
# Preview publishing environment effects
npm run preview

# Preview publishing environment effects + static resource analysis
npm run preview -- --report

# Code format check
npm run lint

# Code format checking and automatic repair
npm run lint -- --fix
```

For more information please refer to [Use documentation](https://panjiachen.github.io/vue-element-admin-site/zh/) 

## Buy Stickers

You can also support vue-element-admin by purchasing [Officially licensed stickers](https://smallsticker.com/product/vue-element-admin) - for every sticker sold, we will receive a donation of 2 yuan.

## Demo

![demo](https://github.com/PanJiaChen/PanJiaChen.github.io/blob/master/images/demo.gif)

## Browsers support

Modern browsers and Internet Explorer 10+.

| [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/edge/edge_48x48.png" alt="IE / Edge" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>IE / Edge | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/firefox/firefox_48x48.png" alt="Firefox" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Firefox | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/chrome/chrome_48x48.png" alt="Chrome" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Chrome | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/safari/safari_48x48.png" alt="Safari" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Safari |
| --------- | --------- | --------- | --------- |
| IE10, IE11, Edge| last 2 versions| last 2 versions| last 2 versions

## License

[MIT](https://github.com/PanJiaChen/vue-admin-template/blob/master/LICENSE) license.

Copyright (c) 2017-present PanJiaChen
