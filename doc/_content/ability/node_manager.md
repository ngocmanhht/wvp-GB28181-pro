<!-- Node management -->

# Node management

! [Node management](_media/img_26.png) 

WVP supports a single WVP multiple ZLM solution to expand WVP's video concurrency capabilities. Concurrent on-demand video is due to bandwidth and performance reasons. A single ZLM node can support a limited number of channels, so WVP adds a ZLM cluster to expand concurrency and ensure the high availability of ZLM.

##Default node

In order to ensure the integrity of functions in WVP, the ZLM node must have at least one default node. This node is not added on the management page, but configured in the WVP configuration file. This node cannot be deleted on the page. Each startup will automatically read the configuration from the configuration file and write it to the standby database.

## Add new node

Start the zlm node you want to add, then click the "Add Node" button to enter the zlm's IP.
http port, SECRET. Click Test to complete the test and start detailed settings for the node. If your zlm is started using docker, the port used by zlm may be inconsistent with the host port, and you need to configure them one by one here.

## The principle of wvp using multiple nodes

WVP will uniformly record the connected nodes in redis and record the load of zlm. When a new request arrives, the zlm with the lowest load will be taken out for use. This ensures node load balancing.
