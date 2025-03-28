from mininet.net import Mininet
from mininet.cli import CLI
from mininet.link import TCLink

def create_topology():
    # Create network with packet loss control
    net = Mininet(link=TCLink)

    # Create two hosts
    h1 = net.addHost('h1')
    h2 = net.addHost('h2')

    # Create link with packet loss
    link = net.addLink(h1, h2)
    
    # Configure loss after creating the link
    link.intf1.config(loss=5)  # 5% packet loss

    net.start()
    CLI(net)
    net.stop()

if __name__ == '__main__':
    create_topology()